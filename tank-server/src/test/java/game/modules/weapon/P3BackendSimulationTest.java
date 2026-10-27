package game.modules.weapon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import framework.PropertyKey;
import framework.game.IGameObject;
import framework.game.IKernel;
import framework.net.ClientMsgDef;
import framework.net.MsgCodec;
import framework.net.SendMessage;
import framework.net.Verify;
import framework.net.message.ClientMsg;
import game.custommsg.CustomMsg;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Local integration fixture, not an Android client or a production Store implementation. */
public class P3BackendSimulationTest implements PropertyKey {
    private static final List<JSONObject> EVIDENCE = new ArrayList<>();
    private static Object oldEncode, oldDecode;
    private static Field encodes, decodes;
    private Harness h;
    private Path archive;
    @Rule public org.junit.rules.TestName testName = new org.junit.rules.TestName();

    @BeforeClass public static void codecTables() throws Exception {
        encodes = Verify.class.getDeclaredField("encodes");
        decodes = Verify.class.getDeclaredField("decodes");
        encodes.setAccessible(true); decodes.setAccessible(true);
        oldEncode = encodes.get(null); oldDecode = decodes.get(null);
        encodes.set(null, Files.readAllBytes(Paths.get("../tank-server/config/encode.bin")));
        decodes.set(null, Files.readAllBytes(Paths.get("../tank-server/config/decode.bin")));
    }

    @AfterClass public static void evidence() throws Exception {
        encodes.set(null, oldEncode); decodes.set(null, oldDecode);
        String directory = System.getProperty("p3.evidenceDirectory");
        if (directory != null) Files.write(Paths.get(directory, "p3-simulation-evidence.json"),
                JSON.toJSONString(EVIDENCE, true).getBytes(StandardCharsets.UTF_8));
    }

    @Before public void setup() throws Exception {
        archive = Files.createTempFile("tank-p3-test-", ".json");
        h = new Harness(archive, testName.getMethodName());
    }

    @After public void cleanup() throws Exception { Files.deleteIfExists(archive); }

    @Test public void normalFlowUsesTopLevelJsonAndAbsolutePlayerMoney() throws Exception {
        h.props.put(PLAYER_MONEY, 4195L);
        h.props.put(PLAYER_MAX_LEVEL_INCOME, 2000L);
        JSONObject verify = h.verify(101);
        assertEquals(600000L, verify.getLongValue("expiresInMs"));
        assertEquals(h.now + 600000L, verify.getLongValue("expiresAt"));
        JSONObject reply = h.claim(102, verify.getString("token"), 4700, false);
        state(reply, 0, "ok", false, false);
        assertTrue(reply.getBooleanValue("granted"));
        assertFalse(reply.getBooleanValue("duplicate"));
        assertEquals(8895L, reply.getLongValue("balance"));
        assertEquals(4700L, reply.getLongValue("rewardCoin"));
        assertEquals("PlayerMoney", reply.getString("balanceProperty"));
        assertEquals(1, h.moneyWrites);
    }

    @Test public void lostAckThenArchiveReloadAndNewReqidPaysExactlyOnce() throws Exception {
        String token = h.verify(201).getString("token");
        h.claim(202, token, 100, true); // Persist first, close transport without delivering ACK.
        assertEquals(1, h.moneyWrites);
        h = h.restarted();
        h.now += 600001L; // Claimed receipt remains valid even after token expiry.
        JSONObject duplicate = h.claim(203, token, 100, false);
        state(duplicate, 0, "already_granted", false, false);
        assertTrue(duplicate.getBooleanValue("duplicate"));
        assertTrue(duplicate.getBooleanValue("granted"));
        assertEquals(100L, h.money());
        assertEquals(0, h.moneyWrites); // Reloaded handler never writes money again.
        assertEquals(100L, duplicate.getLongValue("balance"));
    }

    @Test public void verifySavePendingRetriesSameTokenBeforeThirtySeconds() throws Exception {
        h.saveSucceeds = false;
        JSONObject first = h.verify(301);
        state(first, 8, "player_save_pending", true, false);
        assertFalse(first.getBooleanValue("valid"));
        h.saveSucceeds = true; h.now += 2000;
        JSONObject retry = h.verify(302);
        assertEquals(first.getString("token"), retry.getString("token"));
        assertTrue(retry.getBooleanValue("valid"));
        assertEquals(1, h.props.get("RewardedAdDailyCount"));
        assertEquals(598000L, retry.getLongValue("expiresInMs"));
    }

    @Test public void claimSavePendingRetriesWithoutSecondCreditAndSurvivesReload() throws Exception {
        String token = h.verify(401).getString("token");
        h.saveSucceeds = false;
        JSONObject first = h.claim(402, token, 100, false);
        state(first, 8, "player_save_pending", true, false);
        assertFalse(first.getBooleanValue("granted"));
        assertEquals(100L, h.money());
        h.saveSucceeds = true;
        JSONObject retry = h.claim(403, token, 100, false);
        state(retry, 0, "already_granted", false, false);
        assertEquals(1, h.moneyWrites);
        h = h.restarted();
        assertTrue(h.claim(404, token, 100, false).getBooleanValue("duplicate"));
        assertEquals(100L, h.money());
    }

    @Test public void noSuccessResponseBeforeArchiveCallback() throws Exception {
        String token = h.verify(501).getString("token");
        h.deferSave = true;
        assertNull(h.claim(502, token, 100, false));
        assertNotNull(h.pendingSave);
        assertNull(h.responseFrame);
        h.commitPending(true);
        JSONObject reply = h.decodeResponse(h.responseFrame, 502);
        assertTrue(reply.getBooleanValue("granted"));
        assertEquals(100L, reply.getLongValue("balance"));
    }

    @Test public void strangerExpiredAndOtherUidTokensCannotPay() throws Exception {
        String token = h.verify(601).getString("token");
        state(h.claim(602, "unknown-test-token", 100, false), 23, "token_invalid", false, false);
        Path secondFile = Files.createTempFile("tank-p3-other-", ".json");
        try {
            Harness other = new Harness(secondFile, testName.getMethodName());
            other.props.put(PLAYER_PROPERTY_UID, 999002);
            state(other.claim(603, token, 100, false), 23, "token_invalid", false, false);
            assertEquals(0, other.money());
        } finally { Files.deleteIfExists(secondFile); }
        h.now += 600000;
        state(h.claim(604, token, 100, false), 23, "token_invalid", false, false);
        assertEquals(0, h.moneyWrites);
    }

    @Test public void invalidRewardConsumesTokenAcrossArchiveReload() throws Exception {
        String token = h.verify(701).getString("token");
        JSONObject rejected = h.claim(702, token, 361, false);
        state(rejected, 0, "reward_invalid", false, false);
        assertFalse(rejected.getBooleanValue("granted"));
        h = h.restarted();
        state(h.claim(703, token, 100, false), 23, "token_invalid", false, false);
        assertEquals(0L, h.money());
    }

    @Test public void invalidRewardSaveFailureIsPendingUntilRejectionIsDurable() throws Exception {
        String token = h.verify(711).getString("token");
        h.saveSucceeds = false;
        JSONObject pending = h.claim(712, token, 361, false);
        state(pending, 8, "player_save_pending", true, false);
        assertFalse(pending.getBooleanValue("granted"));
        h.saveSucceeds = true;
        state(h.claim(713, token, 361, false), 23, "token_invalid", false, false);
        h = h.restarted();
        state(h.claim(714, token, 100, false), 23, "token_invalid", false, false);
        assertEquals(0L, h.money());
    }

    @Test public void overflowDoesNotChangeWalletOrConsumeToken() throws Exception {
        String token = h.verify(801).getString("token");
        h.props.put(PLAYER_MONEY, Long.MAX_VALUE);
        state(h.claim(802, token, 100, false), 8, "money_overflow", false, false);
        assertEquals(Long.MAX_VALUE, h.money());
        assertFalse(Boolean.TRUE.equals(h.props.get("RewardedAdTokenClaimed")));
        assertEquals(0, h.moneyWrites);
    }

    @Test public void partialCreditFailurePersistsReviewAndBlocksBothApis() throws Exception {
        String token = h.verify(901).getString("token");
        h.failWrite = "RewardedAdClaimCoin"; // Wallet changed, receipt incomplete.
        state(h.claim(902, token, 100, false), 8, "claim_requires_review", false, true);
        assertEquals(100L, h.money());
        h = h.restarted();
        state(h.claim(903, token, 100, false), 8, "claim_requires_review", false, true);
        state(h.verify(904), 8, "claim_requires_review", false, true);
        assertEquals(100L, h.money());
        assertEquals(0, h.moneyWrites);
    }

    @Test public void bothServerErrorsAreExplicitlyNonRetryable() throws Exception {
        h.failWrite = "RewardedAdDay";
        state(h.verify(1001), 8, "server_error", false, false);
        String token = h.verify(1002).getString("token");
        h.failRead = PLAYER_MONEY;
        state(h.claim(1003, token, 100, false), 8, "server_error", false, false);
        assertEquals(0, h.moneyWrites);
    }

    @Test public void intervalDailyQuotaPendingReuseAndLocalMidnight() throws Exception {
        String token = h.verify(1101).getString("token");
        h.props.put("RewardedAdDailyCount", 20);
        assertEquals(token, h.verify(1102).getString("token"));
        h.claim(1103, token, 100, false);
        JSONObject daily = h.verify(1104);
        state(daily, 0, "ok", false, false);
        assertEquals("frequency_limit", daily.getString("reason"));
        assertEquals("daily", daily.getString("limitType"));
        Calendar midnight = Calendar.getInstance(); midnight.setTimeInMillis(h.now);
        midnight.add(Calendar.DAY_OF_MONTH, 1); midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0); midnight.set(Calendar.SECOND, 0); midnight.set(Calendar.MILLISECOND, 0);
        assertEquals(midnight.getTimeInMillis(), daily.getLongValue("nextAvailableAt"));
        h.props.put("RewardedAdDailyCount", 1);
        h.now += 2000;
        JSONObject interval = h.verify(1105);
        assertEquals("interval", interval.getString("limitType"));
        assertEquals(28000L, interval.getLongValue("retryAfterMs"));
        h.now = midnight.getTimeInMillis();
        assertTrue(h.verify(1106).getBooleanValue("valid"));
        assertEquals(1, h.props.get("RewardedAdDailyCount"));
    }

    @Test public void strictModeRejectsMissingSkippedAndMalformedRequests() throws Exception {
        String old = System.getProperty("rewardedAdRequireCompletionResult");
        try {
            System.setProperty("rewardedAdRequireCompletionResult", "true");
            for (String json : new String[]{"{}", "{\"adResult\":\"Skipped\"}", "{\"adResult\":\"Failed\"}"})
                state(h.exchange(10021, 1201, json, false), 23, "ad_not_completed", false, false);
            state(h.exchange(10021, 1202, "{\"adResult\":123}", false), 6, "invalid_request", false, false);
            assertFalse(h.props.containsKey("RewardedAdToken"));
            String token = h.verify(1203).getString("token");
            state(h.exchange(10022, 1204, "{\"token\":\"" + token + "\",\"rewardCoin\":100.5}", false),
                    6, "invalid_request", false, false);
            assertFalse(Boolean.TRUE.equals(h.props.get("RewardedAdTokenClaimed")));
        } finally {
            if (old == null) System.clearProperty("rewardedAdRequireCompletionResult");
            else System.setProperty("rewardedAdRequireCompletionResult", old);
        }
    }

    @Test public void invalidPlayerHasNoQuotaOrTokenSideEffects() throws Exception {
        h.props.put(PLAYER_PROPERTY_UID, 0);
        state(h.verify(1301), 8, "invalid_player", false, false);
        assertFalse(h.props.containsKey("RewardedAdToken"));
    }

    @Test public void duplicateReceiptSamplesCurrentWalletAndOriginalAmount() throws Exception {
        String token = h.verify(1401).getString("token");
        h.claim(1402, token, 100, false);
        h.props.put(PLAYER_MONEY, 75L); // Other legitimate spending after original claim.
        state(h.claim(1403, token, 200, false), 23, "token_invalid", false, false);
        JSONObject duplicate = h.claim(1404, token, 100, false);
        assertEquals(75L, duplicate.getLongValue("balance"));
        assertEquals(100L, duplicate.getLongValue("rewardCoin"));
        assertTrue(duplicate.getBooleanValue("duplicate"));
        assertEquals(1, h.moneyWrites);
    }

    @Test public void loginKickAndUndefined999UseRealEncoderOverLoopback() throws Exception {
        for (int code = 0; code <= 3; code++) {
            byte[] payload = ClientMsg.LoginRes.newBuilder().setCode(code).setUid(10042).build().toByteArray();
            byte[] decoded = h.systemFrame(1, payload, "constructed_login");
            assertEquals(code, ClientMsg.LoginRes.parseFrom(decoded).getCode());
            if (code == 0) assertArrayEquals(new byte[]{8, 0, 16, (byte) 0xba}, Arrays.copyOf(decoded, 4));
            payload = ClientMsg.Kick.newBuilder().setCode(code).build().toByteArray();
            assertArrayEquals(new byte[]{8, (byte) code}, h.systemFrame(21, payload, "constructed_kick"));
        }
        assertNull(ClientMsgDef.getById(999));
        // Encodable by the generic codec, but no production business semantics are inferred.
        assertEquals(0, h.systemFrame(999, new byte[0], "synthetic_undefined_outer_999").length);
        assertEquals(0, h.systemFrame(999, new byte[0], "synthetic_duplicate_outer_999").length);
        h.systemFrame(21, ClientMsg.Kick.newBuilder().setCode(4).build().toByteArray(), "synthetic_invalid_kick");
        h.systemFrame(21, ClientMsg.Kick.newBuilder().setCode(-1).build().toByteArray(), "synthetic_invalid_kick");
    }

    @Test public void unsupportedClientBranchesHaveExplicitSyntheticWireFixtures() throws Exception {
        String[][] fixtures = {
                {"10021", "{\"code\":0,\"status\":\"ok\",\"valid\":true,\"token\":\"\",\"retryable\":false,\"manualReview\":false}"},
                {"10021", "{\"code\":0,\"status\":\"ok\",\"valid\":false,\"retryable\":false,\"manualReview\":false}"},
                {"10021", "{\"status\":\"ok\",\"valid\":true,\"token\":\"synthetic-placeholder\"}"},
                {"10021", "{\"code\":8,\"status\":\"server_error\",\"valid\":false,\"retryable\":true,\"manualReview\":false}"},
                {"10021", "{\"code\":999,\"valid\":false,\"retryable\":false}"},
                {"10022", "{\"code\":999,\"granted\":false,\"retryable\":false}"},
                {"10022", "{\"code\":0,\"status\":\"unknown_status\",\"granted\":false,\"retryable\":false}"},
                {"10022", "{\"code\":8,\"status\":\"server_error\",\"granted\":false,\"retryable\":true,\"manualReview\":false}"},
                {"10022", "{\"code\":8,\"status\":\"unknown_status\",\"granted\":false,\"retryable\":true}"},
                {"10022", "{\"code\":0,\"status\":\"ok\"}"}
        };
        int reqid = 1500;
        for (String[] fixture : fixtures) {
            reqid++;
            ClientMsg.ResponseMsg response = ClientMsg.ResponseMsg.newBuilder().setReqid(reqid)
                    .setData(ByteString.copyFrom(CustomMsg.String.newBuilder().setValue(fixture[1]).build().toByteArray())).build();
            byte[] transported = h.transportSystemFrame(6, response.toByteArray());
            ClientMsg.ResponseMsg decoded = ClientMsg.ResponseMsg.parseFrom(transported);
            assertEquals(reqid, decoded.getReqid());
            JSONObject json = JSON.parseObject(CustomMsg.String.parseFrom(decoded.getData()).getValue());
            assertEquals(JSON.parseObject(fixture[1]), json);
            h.record(Integer.parseInt(fixture[0]), reqid, json, "synthetic_client_compatibility_only");
        }
    }

    private static void state(JSONObject reply, int code, String status, boolean retryable, boolean review) {
        assertNotNull(reply);
        assertEquals(code, reply.getIntValue("code")); assertEquals(status, reply.getString("status"));
        assertEquals(Boolean.valueOf(retryable), reply.get("retryable"));
        assertEquals(Boolean.valueOf(review), reply.get("manualReview"));
        assertFalse(reply.containsKey("data"));
    }

    private static class Harness {
        final Path archive;
        final String scenario;
        final Map<String, Object> props = new HashMap<>();
        final Map<Object, Object> attributes = new HashMap<>();
        final AdModule ads = new AdModule();
        final IGameObject player;
        final IKernel kernel;
        final IoSession session;
        long now = 1792972800000L; // 2026-10-26 08:00:00 Asia/Shanghai.
        boolean saveSucceeds = true, deferSave;
        String failWrite, failRead;
        int moneyWrites;
        Consumer<Boolean> pendingSave;
        byte[] responseFrame;

        @SuppressWarnings("unchecked") Harness(Path archive, String scenario) throws Exception {
            this.archive = archive; this.scenario = scenario;
            props.put(PLAYER_PROPERTY_UID, 999001); props.put(PLAYER_MONEY, 0L);
            byte[] saved = Files.readAllBytes(archive);
            if (saved.length > 0) props.putAll(JSON.parseObject(new String(saved, StandardCharsets.UTF_8)));
            attributes.put("Type", "Client"); attributes.put("VerifyCode", (short) 5181);
            attributes.put("SendIndex", new AtomicInteger());
            session = (IoSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IoSession.class},
                    (proxy, method, args) -> method.getName().equals("getAttribute") ? attributes.get(args[0]) : zero(method.getReturnType()));
            player = (IGameObject) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IGameObject.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.equals("setProperty")) {
                            if (args[0].equals(failWrite)) { failWrite = null; throw new IllegalStateException("injected write failure"); }
                            if (args[0].equals(PLAYER_MONEY)) moneyWrites++;
                            props.put((String) args[0], args[1]); return zero(method.getReturnType());
                        }
                        if (name.startsWith("get") && args != null && args.length == 1 && args[0] instanceof String) {
                            if (args[0].equals(failRead)) { failRead = null; throw new IllegalStateException("injected read failure"); }
                            Object value = props.get(args[0]);
                            if (method.getReturnType() == long.class) return value == null ? 0L : ((Number) value).longValue();
                            if (method.getReturnType() == int.class) return value == null ? 0 : ((Number) value).intValue();
                            if (method.getReturnType() == boolean.class) return Boolean.TRUE.equals(value);
                            return value;
                        }
                        return zero(method.getReturnType());
                    });
            kernel = (IKernel) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKernel.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getServerTime")) return now;
                        if (method.getName().equals("flushGooglePlayPlayer")) {
                            Consumer<Boolean> cb = (Consumer<Boolean>) args[1];
                            if (deferSave) pendingSave = cb;
                            else { if (saveSucceeds) persist(); cb.accept(saveSucceeds); }
                        }
                        if (method.getName().equals("response")) {
                            ClientMsg.ResponseMsg envelope = ClientMsg.ResponseMsg.newBuilder().setReqid((Integer) args[1])
                                    .setData(ByteString.copyFrom((byte[]) args[2])).build();
                            responseFrame = encode(6, envelope.toByteArray());
                        }
                        return zero(method.getReturnType());
                    });
        }

        Harness restarted() throws Exception { Harness next = new Harness(archive, scenario); next.now = now; return next; }
        long money() { return ((Number) props.get(PLAYER_MONEY)).longValue(); }
        void persist() throws IOException { Files.write(archive, JSON.toJSONString(props).getBytes(StandardCharsets.UTF_8)); }
        void commitPending(boolean saved) throws Exception { if (saved) persist(); pendingSave.accept(saved); pendingSave = null; }
        JSONObject verify(int reqid) throws Exception { return exchange(10021, reqid, "{\"adResult\":\"Finished\"}", false); }
        JSONObject claim(int reqid, String token, long coin, boolean drop) throws Exception {
            JSONObject request = new JSONObject(); request.put("token", token); request.put("rewardCoin", coin);
            return exchange(10022, reqid, request.toJSONString(), drop);
        }

        JSONObject exchange(int businessId, int reqid, String json, boolean drop) throws Exception {
            responseFrame = null;
            ClientMsg.RequestMsg request = ClientMsg.RequestMsg.newBuilder().setMsgid(businessId).setReqid(reqid)
                    .setData(ByteString.copyFrom(CustomMsg.String.newBuilder().setValue(json).build().toByteArray())).build();
            byte[] payload = request.toByteArray();
            IoBuffer body = IoBuffer.allocate(payload.length + 2); body.putShort((short) 5); body.put(payload);
            byte[] encrypted = Verify.encode(reqid, (short) 5181, body.array(), body.array().length);
            try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                 Socket client = new Socket("127.0.0.1", listener.getLocalPort());
                 Socket server = listener.accept()) {
                client.setSoTimeout(3000); server.setSoTimeout(3000);
                DataOutputStream clientOut = new DataOutputStream(client.getOutputStream());
                clientOut.writeInt(encrypted.length + 4); clientOut.writeInt(reqid); clientOut.write(encrypted); clientOut.flush();
                DataInputStream serverIn = new DataInputStream(server.getInputStream());
                int length = serverIn.readInt(), seq = serverIn.readInt();
                byte[] incoming = new byte[length - 4]; serverIn.readFully(incoming);
                IoBuffer clear = IoBuffer.wrap(Verify.decode(seq, (short) 5181, incoming, incoming.length));
                assertEquals(5, clear.getShort()); byte[] data = new byte[clear.remaining()]; clear.get(data);
                ClientMsg.RequestMsg decoded = ClientMsg.RequestMsg.parseFrom(data);
                if (decoded.getMsgid() == 10021) ads.OnReqRewardedAdVerify(kernel, player, decoded.getMsgid(), decoded.getReqid(), decoded.getData().toByteArray());
                else ads.OnReqRewardedAdClaim(kernel, player, decoded.getMsgid(), decoded.getReqid(), decoded.getData().toByteArray());
                if (responseFrame == null) return null; // Archive callback is intentionally held.
                JSONObject reply = decodeResponse(responseFrame, reqid);
                record(businessId, reqid, reply, drop ? "production_handler_ack_dropped" : "production_handler");
                if (drop) return null;
                server.getOutputStream().write(responseFrame); server.getOutputStream().flush();
                return decodeResponse(readServerFrame(client), reqid);
            }
        }

        byte[] encode(int id, byte[] payload) throws Exception {
            final byte[][] frame = new byte[1][];
            ProtocolEncoderOutput out = (ProtocolEncoderOutput) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ProtocolEncoderOutput.class}, (proxy, method, args) -> {
                        if (method.getName().equals("write")) { IoBuffer value = (IoBuffer) args[0]; frame[0] = new byte[value.remaining()]; value.get(frame[0]); }
                        return null;
                    });
            new MsgCodec().encode(session, new SendMessage(id, payload), out); return frame[0];
        }

        byte[] decode(byte[] frame, int id) {
            IoBuffer bytes = IoBuffer.wrap(frame); int length = bytes.getInt(), sequence = bytes.getInt();
            assertEquals(length + 8, frame.length);
            byte[] encrypted = new byte[length]; bytes.get(encrypted);
            IoBuffer body = IoBuffer.wrap(Verify.decode(sequence, (short) 5181, encrypted, encrypted.length));
            assertEquals(id, body.getShort()); byte[] payload = new byte[body.remaining()]; body.get(payload); return payload;
        }

        JSONObject decodeResponse(byte[] frame, int reqid) throws Exception {
            ClientMsg.ResponseMsg envelope = ClientMsg.ResponseMsg.parseFrom(decode(frame, 6));
            assertEquals(reqid, envelope.getReqid());
            return JSON.parseObject(CustomMsg.String.parseFrom(envelope.getData()).getValue());
        }

        byte[] transportSystemFrame(int id, byte[] payload) throws Exception {
            byte[] frame = encode(id, payload);
            try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
                 Socket client = new Socket("127.0.0.1", listener.getLocalPort()); Socket server = listener.accept()) {
                client.setSoTimeout(3000);
                server.getOutputStream().write(frame); server.getOutputStream().flush();
                return decode(readServerFrame(client), id);
            }
        }

        byte[] systemFrame(int id, byte[] payload, String source) throws Exception {
            byte[] decoded = transportSystemFrame(id, payload);
            JSONObject meta = new JSONObject(); meta.put("outerMsgId", id); meta.put("frameLength", payload.length + 10);
            StringBuilder hex = new StringBuilder(); for (byte b : decoded) hex.append(String.format("%02X ", b & 255));
            meta.put("payloadHex", hex.toString().trim()); record(id, 0, meta, source);
            return decoded;
        }

        static byte[] readServerFrame(Socket client) throws IOException {
            DataInputStream in = new DataInputStream(client.getInputStream()); int length = in.readInt();
            byte[] tail = new byte[length + 4]; in.readFully(tail);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(buffer);
            out.writeInt(length); out.write(tail); return buffer.toByteArray();
        }

        void record(int msgid, int reqid, JSONObject reply, String source) {
            JSONObject clean = JSON.parseObject(reply.toJSONString());
            String token = clean.getString("token");
            if (token != null) clean.put("token", "<redacted:len=" + token.length() + ">");
            String ref = clean.getString("rewardReference");
            if (ref != null) clean.put("rewardReference", "<fingerprint:" + ref.substring(0, 8) + ">");
            JSONObject event = new JSONObject(true); event.put("scenario", scenario); event.put("source", source);
            event.put("msgid", msgid); event.put("reqid", reqid); event.put("serverTimeMs", now); event.put("response", clean);
            EVIDENCE.add(event);
        }
    }

    private static Object zero(Class<?> type) {
        if (type == boolean.class) return false; if (type == int.class) return 0; if (type == long.class) return 0L;
        if (type == float.class) return 0F; if (type == double.class) return 0D; return null;
    }
}
