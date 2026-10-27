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
        assertFalse(waiter.tick(now + 399999L));
        sessions(gate, "loginSessions").put(42, session(42));
        assertTrue(waiter.tick(now + 400001L));
        assertEquals(0, server.disconnects);
    }

    @Test
    public void abandonedPlayerIsDisconnectedOnlyAfterGraceExpires() throws Exception {
        GateLogic gate = new GateLogic();
        RecordingServer server = attachServer(gate);
        GateLogic.DisconnectWaiter waiter = gate.new DisconnectWaiter("game1", 42);
        assertFalse(waiter.tick(1L));
        assertFalse(waiter.tick(400000L));
        assertTrue(waiter.tick(400002L));
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

        @Override public ServerSet getServerSet() { return servers; }
        @Override public int getFrontPort() { return 0; }

        @Override
        public void sendMsgToServer(String server, int msgid, byte[] bytes) {
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
