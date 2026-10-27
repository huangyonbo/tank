package framework.logic;

import framework.BaseServer;
import framework.SpringContextUtil;
import framework.game.GameObject;
import framework.game.GamePlayer;
import framework.game.IGameObject;
import framework.game.Kernel;
import framework.mybatis.service.impl.InviteService;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class GameLogicLoginTest {
    private Object previousContext;
    private Field context;
    private GameLogic game;
    private final FailingPlayer player = new FailingPlayer();
    private final RecordingServer server = new RecordingServer();
    private boolean destroyed;

    @Before
    public void setup() throws Exception {
        context = SpringContextUtil.class.getDeclaredField("applicationContext");
        context.setAccessible(true);
        previousContext = context.get(null);
        InviteService invites = new InviteService() {
            @Override public int searchBingId(int uid) { return 0; }
        };
        context.set(null, Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ApplicationContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getBean")) return invites;
                    return null;
                }));
        Kernel kernel = new Kernel() {
            @Override public GamePlayer getPlayer(int uid, boolean findList) { return null; }
            @Override public GameObject createObjectByScript(String script) { return player; }
            @Override public void destroyGameObject(IGameObject object) { destroyed = true; }
        };
        game = new GameLogic();
        put("baseServer", server);
        put("kernel", kernel);
        put("logger", LoggerFactory.getLogger(GameLogic.class));
    }

    @After
    public void restoreContext() throws Exception { context.set(null, previousContext); }

    @Test
    public void archiveLoadExceptionReturnsFailureInsteadOfZeroFilledSuccess() throws Exception {
        server.roleCode = StoreLogic.LoadRoleCode.SUCCESS.ordinal();
        game.onRecLoadPlayer(100, InnerMsg.LoadPlayer.newBuilder().setUid(42).build().toByteArray());
        assertArrayEquals(new byte[]{2}, server.response);
        assertTrue(destroyed);
    }

    @Test
    public void newPlayerInitializationExceptionReturnsFailureInsteadOfSuccess() throws Exception {
        server.roleCode = StoreLogic.LoadRoleCode.NOROLE.ordinal();
        game.onRecLoadPlayer(100, InnerMsg.LoadPlayer.newBuilder().setUid(42).build().toByteArray());
        assertArrayEquals(new byte[]{2}, server.response);
        assertTrue(destroyed);
    }

    private void put(String name, Object value) throws Exception {
        Field field = GameLogic.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }

    private static class FailingPlayer extends GamePlayer {
        FailingPlayer() { super(null); }
        @Override public void setProperty(String key, Object value) { }
        @Override public boolean loadFromArchive(IoBuffer buffer) {
            throw new IllegalStateException("injected archive failure");
        }
        @Override public void storeToArchive(IoBuffer buffer) { }
        @Override public void onLoad() { throw new IllegalStateException("injected new player failure"); }
    }

    private static class RecordingServer extends BaseServer {
        int roleCode;
        byte[] response;
        @Override public State GetState() { return State.RUN; }
        @Override public void response(int reqid, byte[] bytes) { response = bytes; }
        @Override public void request(String server, int msgid, byte[] bytes, Consumer<byte[]> callback) {
            assertEquals("Store", server);
            assertTrue(msgid == InnerMsgDef.INNER_MSG_REQUEST_ROLE_DATA.ordinal()
                    || msgid == InnerMsgDef.INNER_MSG_REG_ROLE_DATA.ordinal());
            callback.accept(InnerMsg.LoadRoleData.newBuilder().setCode(roleCode).build().toByteArray());
        }
    }
}
