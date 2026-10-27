package framework.net;

import framework.net.message.ClientMsg;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ClientWireProtocolTest {
    @Test
    public void keyFrameIsCleartextAndHasSeparateLengthAndSequence() throws Exception {
        Map<Object, Object> attributes = attributes();
        byte[] keyPayload = new byte[128];
        IoBuffer frame = encode(attributes, new SendMessage(20, keyPayload));
        assertEquals(138, frame.remaining());
        assertEquals(130, frame.getInt()); // S -> C length excludes the 4-byte sequence.
        assertEquals(1, frame.getInt());
        assertEquals(20, frame.getShort());
        assertEquals(128, frame.remaining());
    }

    @Test
    public void kickHeaderMustBeDecodedWithCurrentSocketKey() throws Exception {
        Field encodes = Verify.class.getDeclaredField("encodes");
        Field decodes = Verify.class.getDeclaredField("decodes");
        encodes.setAccessible(true);
        decodes.setAccessible(true);
        Object oldEncode = encodes.get(null), oldDecode = decodes.get(null);
        try {
            encodes.set(null, Files.readAllBytes(Paths.get("../tank-server/config/encode.bin")));
            decodes.set(null, Files.readAllBytes(Paths.get("../tank-server/config/decode.bin")));
            Map<Object, Object> attributes = attributes();
            byte[] payload = ClientMsg.Kick.newBuilder().setCode(2).build().toByteArray();
            IoBuffer frame = encode(attributes, new SendMessage(21, payload));
            int length = frame.getInt(), sequence = frame.getInt();
            byte[] encoded = new byte[length];
            frame.get(encoded);
            byte[] decoded = Verify.decode(sequence, (short) attributes.get("VerifyCode"), encoded, length);
            IoBuffer body = IoBuffer.wrap(decoded);
            assertEquals(21, body.getShort());
            byte[] protobuf = new byte[body.remaining()];
            body.get(protobuf);
            assertEquals(2, ClientMsg.Kick.parseFrom(protobuf).getCode());
        } finally {
            encodes.set(null, oldEncode);
            decodes.set(null, oldDecode);
        }
    }

    @Test
    public void staleKeysCanProduceBothReportedNegativeIdsFromCleartextKeyHeader() throws Exception {
        Field decodes = Verify.class.getDeclaredField("decodes");
        decodes.setAccessible(true);
        Object oldDecode = decodes.get(null);
        try {
            decodes.set(null, Files.readAllBytes(Paths.get("../tank-server/config/decode.bin")));
            for (int reported : new int[]{-20906, -20215}) {
                boolean reproduced = false;
                for (int key = Short.MIN_VALUE; key <= Short.MAX_VALUE; key++) {
                    byte[] wrong = Verify.decode(1, (short) key, new byte[]{0, 20}, 2);
                    short wrongId = (short) (((wrong[0] & 255) << 8) | (wrong[1] & 255));
                    if (wrongId == reported) {
                        reproduced = true;
                        break;
                    }
                }
                assertTrue("clear CLIENT_KEY header can be misdecoded as " + reported, reproduced);
            }
        } finally {
            decodes.set(null, oldDecode);
        }
    }

    @Test
    public void sixteenByteLoginResponseMatchesReportedD07Payload() throws Exception {
        ClientMsg.LoginRes login = ClientMsg.LoginRes.newBuilder().setCode(0).setUid(10039)
                .setName("10039").setHead("").setSex(0).build();
        assertEquals(16, login.toByteArray().length);
        ClientMsg.LoginRes decoded = ClientMsg.LoginRes.parseFrom(login.toByteArray());
        assertEquals(0, decoded.getCode());
        assertEquals(10039, decoded.getUid());
        assertEquals("10039", decoded.getName());
    }

    @Test
    public void p3LoginSuccessExplicitlySerializesZeroAndKickHasOnlyCode() throws Exception {
        ClientMsg.LoginRes login = ClientMsg.LoginRes.newBuilder().setCode(0).setUid(10042).build();
        assertTrue(login.hasCode());
        assertArrayEquals(new byte[]{8, 0, 16, (byte) 0xba},
                java.util.Arrays.copyOf(login.toByteArray(), 4));
        assertTrue(login.getDescriptorForType().findFieldByNumber(1).isOptional());
        for (int code = 0; code <= 3; code++) {
            ClientMsg.Kick kick = ClientMsg.Kick.newBuilder().setCode(code).build();
            assertArrayEquals(new byte[]{8, (byte) code}, kick.toByteArray());
            assertEquals(1, kick.getDescriptorForType().getFields().size());
        }
    }

    @Test
    public void reportedUnknownIdsHaveNoOuterProtocolDefinition() {
        assertNull(ClientMsgDef.getById(999));
        assertNull(ClientMsgDef.getById(-20906));
        assertNull(ClientMsgDef.getById(-20215));
    }

    private static Map<Object, Object> attributes() {
        Map<Object, Object> values = new HashMap<>();
        values.put("Type", "Client");
        values.put("SendIndex", new AtomicInteger());
        values.put("VerifyCode", (short) 5181);
        return values;
    }

    private static IoBuffer encode(Map<Object, Object> attributes, SendMessage message) throws Exception {
        IoSession session = (IoSession) Proxy.newProxyInstance(ClientWireProtocolTest.class.getClassLoader(),
                new Class<?>[]{IoSession.class}, (proxy, method, args) -> attributes.get(args[0]));
        AtomicReference<IoBuffer> written = new AtomicReference<>();
        ProtocolEncoderOutput output = (ProtocolEncoderOutput) Proxy.newProxyInstance(
                ClientWireProtocolTest.class.getClassLoader(), new Class<?>[]{ProtocolEncoderOutput.class},
                (proxy, method, args) -> { if (method.getName().equals("write")) written.set((IoBuffer) args[0]); return null; });
        new MsgCodec().encode(session, message, output);
        return written.get();
    }
}
