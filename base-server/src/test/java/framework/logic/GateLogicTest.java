package framework.logic;

import framework.BaseServer;
import framework.ServerSet;
import framework.net.ClientMsgDef;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.ClientMsg;
import framework.net.message.InnerMsg;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import static framework.PropertyKey.*;
import static org.junit.Assert.*;

public class GateLogicTest {
    @Test
    public void closingOldSocketCannotRemoveNewSession() throws Exception {
        GateLogic gate = new GateLogic();
        IoSession old = session(42);
        IoSession replacement = session(42);
        sessions(gate, "loginSessions").put(42, replacement);
        assertTrue(gate.onSessionClosed(old));
        assertSame(replacement, sessions(gate, "loginSessions").get(42));
        assertTrue(sessions(gate, "disconnectCache").isEmpty());
    }

    @Test
    public void closingOldSocketCannotRemovePendingLogin() throws Exception {
        GateLogic gate = new GateLogic();
        IoSession old = session(42);
        IoSession replacement = session(42);
        sessions(gate, "waitLoginSessions").put(42, replacement);
        gate.onSessionClosed(old);
        assertSame(replacement, sessions(gate, "waitLoginSessions").get(42));
        gate.onSessionClosed(replacement);
        assertTrue(sessions(gate, "waitLoginSessions").isEmpty());
    }

    @Test
    public void closingCurrentSessionStartsDisconnectWait() throws Exception {
        GateLogic gate = new GateLogic();
        IoSession current = session(42);
        sessions(gate, "loginSessions").put(42, current);
        sessions(gate, "waitLoginSessions").put(42, current);
        gate.onSessionClosed(current);
        assertTrue(sessions(gate, "loginSessions").isEmpty());
        assertTrue(sessions(gate, "waitLoginSessions").isEmpty());
        assertTrue(sessions(gate, "disconnectCache").containsKey(42));
    }

    @Test
    public void staleLoginCallbackCannotAffectReplacement() throws Exception {
        GateLogic gate = new GateLogic();
        IoSession replacement = session(42);
        sessions(gate, "waitLoginSessions").put(42, replacement);
        gate.onCheckLogin(null, session(42));
        assertSame(replacement, sessions(gate, "waitLoginSessions").get(42));
    }

    @Test
    public void resumeMustUseAuthenticatedLoginPath() {
        class TrackingGate extends GateLogic {
            boolean called;
            @Override
            void onRecRequestLogin(IoSession session, byte[] bytes) { called = true; }
        }
        TrackingGate gate = new TrackingGate();
        gate.onClientResume(session(42), new byte[0]);
        assertTrue(gate.called);
    }

    @Test
    public void pendingGameLoadCannotForwardEitherBusinessMessage() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        for (boolean request : new boolean[]{true, false}) {
            IoSession pending = session(42);
            sessions(gate, "loginSessions").put(42, pending);
            if (request) gate.onRecRequestMsg(pending, new byte[]{1});
            else gate.onRecCustomMsg(pending, new byte[]{1});
            assertFalse(pending.isConnected());
        }
        assertEquals(0, server.forwarded);
    }

    @Test
    public void readyCurrentSocketForwardsBothBusinessMessages() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        IoSession ready = session(42);
        ready.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        sessions(gate, "loginSessions").put(42, ready);
        gate.onRecRequestMsg(ready, new byte[]{1});
        assertEquals(ClientMsgDef.CLIENT_REQUEST.ordinal(), server.last.getMsgid());
        gate.onRecCustomMsg(ready, new byte[]{2});
        assertEquals(ClientMsgDef.CLIENT_CUSTOM.ordinal(), server.last.getMsgid());
        assertEquals(42, server.last.getUid());
        assertArrayEquals(new byte[]{2}, server.last.getData().toByteArray());
        assertEquals(2, server.forwarded);
        assertTrue(ready.isConnected());
    }

    @Test
    public void oldReadySocketCannotSendAfterReplacement() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        IoSession old = session(42);
        old.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        IoSession replacement = session(42);
        sessions(gate, "loginSessions").put(42, replacement);
        gate.onRecRequestMsg(old, new byte[]{1});
        assertEquals(0, server.forwarded);
        assertFalse(old.isConnected());
        assertTrue(replacement.isConnected());
        assertSame(replacement, sessions(gate, "loginSessions").get(42));
    }

    @Test
    public void gameSelectionSkipsMissingDisconnectedAndUnreportedServers() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        Map<String, IoSession> games = new HashMap<>();
        IoSession unreported = session(0);
        IoSession disconnected = session(0);
        disconnected.setAttribute("Load", 0);
        disconnected.close(true);
        IoSession busy = session(0); busy.setAttribute("Load", 9);
        IoSession free = session(0); free.setAttribute("Load", 2);
        games.put("unreported", unreported); games.put("disconnected", disconnected);
        games.put("busy", busy); games.put("free", free);
        server.servers = new ServerSet("test") {
            @Override public Object[] getServersByType(String type) {
                return new Object[]{"missing", "unreported", "disconnected", "busy", "free"};
            }
            @Override public IoSession getServer(String name) { return games.get(name); }
        };
        assertEquals("free", gate.getCurGame());
        games.clear();
        assertNull(gate.getCurGame());
    }

    @Test
    public void heartbeatDoesNotReuseLoginTicketOrReauthenticateLiveSession() throws Exception {
        GateLogic gate = new GateLogic();
        attachServer(gate);
        int[] authenticationRequests = {0};
        Field api = GateLogic.class.getDeclaredField("httpClientApi");
        api.setAccessible(true);
        api.set(gate, Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{framework.net.http.HttpClientApi.class}, (proxy, method, args) -> {
                    authenticationRequests[0]++;
                    return null;
                }));
        IoSession current = session(42);
        current.setAttribute("Token", "already-consumed-ticket");
        current.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        sessions(gate, "loginSessions").put(42, current);
        for (int i = 0; i < 3; i++) gate.onRecHeartBeat(current, null);
        assertEquals(0, authenticationRequests[0]);
        assertTrue(current.isConnected());
        assertTrue((Boolean) current.getAttribute(GateLogic.CLIENT_LOGIN_READY));
        assertSame(current, sessions(gate, "loginSessions").get(42));
        @SuppressWarnings("unchecked")
        List<SendMessage> replies = (List<SendMessage>) current.getAttribute(GateLogic.CLINET_ATT_KEY);
        assertEquals(3, replies.size());
        for (SendMessage reply : replies) assertEquals(ClientMsgDef.CLIENT_HEART_BEAT.ordinal(), reply.msgID);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rolePushCannotReachClientBeforeGameLoadIsReady() throws Exception {
        GateLogic gate = new GateLogic();
        attachServer(gate);
        IoSession current = session(42);
        sessions(gate, "loginSessions").put(42, current);
        InnerMsg.ForwardMsg role = InnerMsg.ForwardMsg.newBuilder().setUid(42)
                .setMsgid(ClientMsgDef.CLIENT_LOAD_OBJECT.ordinal()).build();
        gate.onRecForwardMsg(null, role.toByteArray());
        gate.execute();
        assertNull(current.getAttribute("written"));
        current.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        gate.execute();
        List<SendMessage> sent = (List<SendMessage>) current.getAttribute("written");
        assertEquals(1, sent.size());
        assertEquals(2, sent.get(0).msgID);
    }

    @Test
    public void timeoutPreservesReconnectGraceAndStaleWaiterCannotDestroyReplacement() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        IoSession current = session(42);
        current.setAttribute("lastHeartBeat", System.currentTimeMillis() - 181000L);
        current.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        sessions(gate, "loginSessions").put(42, current);
        gate.execute();
        assertFalse(current.isConnected());
        assertTrue(sessions(gate, "loginSessions").isEmpty());
        GateLogic.DisconnectWaiter waiter = (GateLogic.DisconnectWaiter)
                sessions(gate, "disconnectCache").get(42);
        assertNotNull(waiter);
        assertEquals(0, server.disconnects);
        long now = System.currentTimeMillis();
        assertFalse(waiter.tick(now));
        assertFalse(waiter.tick(now + 119000L));
        sessions(gate, "loginSessions").put(42, session(42));
        assertTrue(waiter.tick(now + 120001L));
        assertEquals(0, server.disconnects);
    }

    @Test
    public void abandonedPlayerIsDisconnectedOnlyAfterGraceExpires() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        GateLogic.DisconnectWaiter waiter = gate.new DisconnectWaiter("game1", 42);
        assertFalse(waiter.tick(1L));
        assertFalse(waiter.tick(120000L));
        assertTrue(waiter.tick(120002L));
        assertEquals(1, server.disconnects);
    }

    @Test
    public void bothHandshakeIdsRecoverThreeTimesWithConsumedTicketAndNoKick() throws Exception {
        for (boolean resume : new boolean[]{false, true}) {
            GateLogic gate = new GateLogic();
            RecordingServer server = attachServer(gate);
            IoSession game = session(0);
            server.servers = new ServerSet("test") {
                @Override public IoSession getServer(String name) { return game; }
            };
            IoSession current = session(42);
            current.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
            current.setAttribute(GateLogic.RESUME_GRANT, grant(42));
            sessions(gate, "loginSessions").put(42, current);
            for (long offlineMs : new long[]{5000, 12000, 30000}) {
                current.close(true);
                gate.onSessionClosed(current);
                GateLogic.DisconnectWaiter waiter = (GateLogic.DisconnectWaiter) sessions(gate, "disconnectCache").get(42);
                waiter.time -= offlineMs;
                IoSession next = session(42);
                next.setAttribute("Type", "Client");
                byte[] login = ClientMsg.Login.newBuilder().setId(42).setUid(42).setToken("consumed")
                        .setDeviceId("device-42").build().toByteArray();
                if (resume) gate.onClientResume(next, login); else gate.onRecRequestLogin(next, login);
                assertEquals(42, server.lastLoad.getUid());
                assertFalse(Boolean.TRUE.equals(next.getAttribute(GateLogic.CLIENT_LOGIN_READY)));
                assertTrue(sessions(gate, "waitLoginSessions").isEmpty());
                server.loadCallback.accept(new byte[]{0});
                gate.execute();
                assertTrue(Boolean.TRUE.equals(next.getAttribute(GateLogic.CLIENT_LOGIN_READY)));
                assertSame(next, sessions(gate, "loginSessions").get(42));
                assertTrue(sessions(gate, "disconnectCache").isEmpty());
                assertNull(next.getAttribute("Token"));
                @SuppressWarnings("unchecked")
                List<SendMessage> sent = (List<SendMessage>) next.getAttribute("written");
                assertEquals(1, sent.size());
                assertEquals(1, sent.get(0).msgID);
                assertEquals(42, ClientMsg.LoginRes.parseFrom(sent.get(0).data).getUid());
                current = next;
            }
            assertEquals(3, server.loads);
            assertEquals(0, server.disconnects);
        }
    }

    @Test
    public void liveRecoveryClosesOldSocketSilentlyAndIgnoresStaleLoadCallback() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        IoSession game = session(0);
        server.servers = new ServerSet("test") {
            @Override public IoSession getServer(String name) { return game; }
        };
        IoSession old = session(42);
        old.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        old.setAttribute(GateLogic.RESUME_GRANT, grant(42));
        sessions(gate, "loginSessions").put(42, old);
        IoSession next = session(42); next.setAttribute("Type", "Client");
        gate.onRecRequestLogin(next, ClientMsg.Login.newBuilder().setId(42).setToken("consumed")
                .setDeviceId("device-42").build().toByteArray());
        assertFalse(old.isConnected());
        assertNull(old.getAttribute("written"));
        gate.onSessionClosed(old);
        assertSame(next, sessions(gate, "loginSessions").get(42));
        assertTrue(sessions(gate, "disconnectCache").isEmpty());
        IoSession replacement = session(42);
        sessions(gate, "loginSessions").put(42, replacement);
        server.loadCallback.accept(new byte[]{0});
        assertFalse(Boolean.TRUE.equals(next.getAttribute(GateLogic.CLIENT_LOGIN_READY)));
        assertSame(replacement, sessions(gate, "loginSessions").get(42));
    }

    @Test
    public void recoveryProofRejectsWrongTicketDeviceAndBlankDevice() {
        GateLogic.ResumeGrant grant = grant(42);
        ClientMsg.Login.Builder login = ClientMsg.Login.newBuilder().setId(42).setToken("consumed").setDeviceId("device-42");
        assertTrue(grant.matches(login.build()));
        assertFalse(grant.matches(login.setToken("wrong").build()));
        assertFalse(grant.matches(login.setToken("consumed").setDeviceId("other-device").build()));
        assertFalse(grant.matches(login.setDeviceId("").build()));
        assertEquals("missing_device_id", grant.mismatch(login.build()));
        assertEquals("device_changed", grant.mismatch(login.setDeviceId("other-device").build()));
        assertEquals("ticket_changed", grant.mismatch(login.setDeviceId("device-42").setToken("other-ticket").build()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void undefinedOuter999IsBlockedWhileBusinessCode999IsStillDelivered() throws Exception {
        GateLogic gate = new GateLogic(); attachServer(gate);
        IoSession current = session(42); current.setAttribute(GateLogic.CLIENT_LOGIN_READY,true);
        sessions(gate,"loginSessions").put(42,current);
        gate.onRecForwardMsg(null,InnerMsg.ForwardMsg.newBuilder().setUid(42).setMsgid(999).build().toByteArray());
        gate.execute(); assertNull(current.getAttribute("written"));
        gate.onRecForwardMsg(null,InnerMsg.ForwardMsg.newBuilder().setUid(42)
                .setMsgid(ClientMsgDef.CLIENT_RESPONSE.ordinal()).setData(com.google.protobuf.ByteString.copyFromUtf8("{code:999}")).build().toByteArray());
        gate.execute();
        List<SendMessage> sent=(List<SendMessage>)current.getAttribute("written");
        assertEquals(1,sent.size()); assertEquals(ClientMsgDef.CLIENT_RESPONSE.ordinal(),sent.get(0).msgID);
    }

    @Test
    public void gameRefusalRevokesRecoveryAndCannotEnableBusiness() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        IoSession game = session(0);
        server.servers = new ServerSet("test") {
            @Override public IoSession getServer(String name) { return game; }
        };
        IoSession old = session(42);
        old.setAttribute(GateLogic.RESUME_GRANT, grant(42));
        sessions(gate, "loginSessions").put(42, old);
        gate.onSessionClosed(old);
        IoSession next = session(42); next.setAttribute("Type", "Client");
        gate.onClientResume(next, ClientMsg.Login.newBuilder().setId(42).setToken("consumed")
                .setDeviceId("device-42").build().toByteArray());
        server.loadCallback.accept(new byte[]{1});
        assertNull(next.getAttribute(GateLogic.RESUME_GRANT));
        assertFalse(Boolean.TRUE.equals(next.getAttribute(GateLogic.CLIENT_LOGIN_READY)));
        assertTrue(sessions(gate, "loginSessions").isEmpty());
        assertTrue(sessions(gate, "disconnectCache").isEmpty());
    }

    private static GateLogic.ResumeGrant grant(int uid) {
        return new GateLogic.ResumeGrant("consumed", "device-42", "game1",
                InnerMsg.LoadPlayer.newBuilder().setUid(uid).build().toByteArray(),
                ClientMsg.LoginRes.newBuilder().setCode(0).setUid(uid).build().toByteArray());
    }

    @Test
    public void expiredOrDifferentProofCannotReplaceCurrentOwner() throws Exception {
        GateLogic gate = new GateLogic();
        IoSession old = session(42);
        old.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        old.setAttribute(GateLogic.RESUME_GRANT, grant(42));
        sessions(gate, "loginSessions").put(42, old);
        java.lang.reflect.Method resume = GateLogic.class.getDeclaredMethod("tryResume", IoSession.class, ClientMsg.Login.class);
        resume.setAccessible(true);
        ClientMsg.Login.Builder login = ClientMsg.Login.newBuilder().setId(42).setToken("wrong").setDeviceId("device-42");
        assertEquals(false, resume.invoke(gate, session(42), login.build()));
        assertTrue(old.isConnected());
        assertSame(old, sessions(gate, "loginSessions").get(42));
        old.close(true); gate.onSessionClosed(old);
        GateLogic.DisconnectWaiter waiter = (GateLogic.DisconnectWaiter) sessions(gate, "disconnectCache").get(42);
        waiter.time = System.currentTimeMillis() - 120001L;
        assertEquals(false, resume.invoke(gate, session(42), login.setToken("consumed").build()));
    }

    @Test
    public void loadTimeoutKeepsBoundedRecoveryProofButAdminKickRevokesIt() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        IoSession game = session(0);
        server.servers = new ServerSet("test") {
            @Override public IoSession getServer(String name) { return game; }
        };
        IoSession old = session(42);
        old.setAttribute(GateLogic.CLIENT_LOGIN_READY, true);
        old.setAttribute(GateLogic.RESUME_GRANT, grant(42));
        sessions(gate, "loginSessions").put(42, old);
        IoSession next = session(42); next.setAttribute("Type", "Client");
        gate.onRecRequestLogin(next, ClientMsg.Login.newBuilder().setId(42).setToken("consumed")
                .setDeviceId("device-42").build().toByteArray());
        server.loadCallback.accept(null);
        assertTrue(sessions(gate, "loginSessions").isEmpty());
        GateLogic.DisconnectWaiter waiter = (GateLogic.DisconnectWaiter) sessions(gate, "disconnectCache").get(42);
        assertNotNull(waiter.grant);
        gate.onRecKickPlayer(null, InnerMsg.KickPlayer.newBuilder().setUid(42).setCode(1).build().toByteArray());
        assertTrue(sessions(gate, "disconnectCache").isEmpty());
        assertEquals(1, server.disconnects);
    }

    @Test
    public void p3KickCodesDisableBusinessAndCloseAfterWriteCompletes() throws Exception {
        for (int code = 0; code <= 3; code++) {
            GateLogic gate = new GateLogic();
            Map<Object, Object> attributes = new HashMap<>();
            attributes.put("connected", true);
            attributes.put(GateLogic.CLIENT_LOGIN_READY, true);
            final SendMessage[] written = new SendMessage[1];
            final DefaultWriteFuture[] future = new DefaultWriteFuture[1];
            IoSession current = (IoSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IoSession.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getAttribute")) return attributes.get(args[0]);
                        if (method.getName().equals("setAttribute")) return attributes.put(args[0], args[1]);
                        if (method.getName().equals("write")) {
                            written[0] = (SendMessage) args[0];
                            future[0] = new DefaultWriteFuture((IoSession) proxy);
                            return future[0];
                        }
                        if (method.getName().equals("close")) { attributes.put("connected", false); return null; }
                        if (method.getName().equals("isConnected")) return attributes.get("connected");
                        return null;
                    });
            gate.kickPlayer(current, code);
            assertFalse((Boolean) attributes.get(GateLogic.CLIENT_LOGIN_READY));
            assertTrue(current.isConnected());
            assertEquals(21, written[0].msgID);
            assertTrue(written[0].close);
            assertEquals(code, ClientMsg.Kick.parseFrom(written[0].data).getCode());
            future[0].setWritten();
            assertFalse(current.isConnected());
        }
    }

    private static RecordingServer attachServer(GateLogic gate) throws Exception {
        RecordingServer server = new RecordingServer();
        Field field = GateLogic.class.getDeclaredField("baseServer");
        field.setAccessible(true);
        field.set(gate, server);
        return server;
    }

    private static class RecordingServer extends BaseServer {
        int forwarded;
        int disconnects;
        ServerSet servers;
        InnerMsg.ForwardMsg last;
        InnerMsg.LoadPlayer lastLoad;
        Consumer<byte[]> loadCallback;
        int loads;

        @Override public ServerSet getServerSet() { return servers; }
        @Override public int getFrontPort() { return 0; }

        @Override public void request(String game, int msgid, byte[] data, long timeoutMs, Consumer<byte[]> callback) {
            assertEquals(InnerMsgDef.INNER_MSG_LOAD_PLAYER.ordinal(), msgid);
            assertTrue(timeoutMs > 0);
            try { lastLoad = InnerMsg.LoadPlayer.parseFrom(data); }
            catch (Exception invalid) { throw new AssertionError(invalid); }
            loads++;
            loadCallback = callback;
        }

        @Override
        public void sendMsgToServer(String server, int msgid, byte[] bytes) {
            if (msgid == InnerMsgDef.INNER_MSG_FROZEN.ordinal()) {
                assertEquals(ServerSet.SERVER_LOGIC_NAME_STORE, server);
                return;
            }
            assertEquals("game1", server);
            if (msgid == InnerMsgDef.INNER_MSG_CLIENT_DISCONNECT.ordinal()) {
                disconnects++;
                return;
            }
            assertEquals(InnerMsgDef.INNER_MSG_FORWARD.ordinal(), msgid);
            try {
                last = InnerMsg.ForwardMsg.parseFrom(bytes);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            forwarded++;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Object> sessions(GateLogic gate, String fieldName) throws Exception {
        Field field = GateLogic.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<Integer, Object>) field.get(gate);
    }

    private static IoSession session(int uid) {
        Map<Object, Object> attributes = new HashMap<>();
        attributes.put(PLAYER_PROPERTY_ID, uid);
        attributes.put("BackSer", "game1");
        attributes.put("connected", true);
        return (IoSession) Proxy.newProxyInstance(GateLogicTest.class.getClassLoader(),
                new Class<?>[]{IoSession.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getAttribute")) return attributes.get(args[0]);
                    if (method.getName().equals("setAttribute")) return attributes.put(args[0], args[1]);
                    if (method.getName().equals("removeAttribute")) return attributes.remove(args[0]);
                    if (method.getName().equals("write")) {
                        @SuppressWarnings("unchecked")
                        List<SendMessage> sent = (List<SendMessage>) attributes.computeIfAbsent("written", k -> new ArrayList<SendMessage>());
                        sent.add((SendMessage) args[0]);
                        return new DefaultWriteFuture((IoSession) proxy);
                    }
                    if (method.getName().equals("close")) { attributes.put("connected", false); return null; }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("isConnected")) return attributes.get("connected");
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                });
    }
}
