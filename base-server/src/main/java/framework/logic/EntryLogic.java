package framework.logic;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.BaseServer;
import framework.ILogic;
import framework.ServerConfig;
import framework.ServerSet;
import framework.net.ClientMsgDef;
import framework.net.InnerMsgDef;
import framework.net.SendMessage;
import framework.net.message.ClientMsg;
import framework.net.message.InnerMsg;
import org.apache.logging.log4j.util.Strings;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

@Component
public class EntryLogic implements ILogic {
    BaseServer m_baseServer;
    private Logger logger = LoggerFactory.getLogger(EntryLogic.class);

    @Override
    public boolean onInit(BaseServer ser) {
        m_baseServer = ser;
        m_baseServer.addClientMsgListener(this, ClientMsgDef.CLIENT_GET_ADDR.ordinal(), "onRecAllocGate");
        m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_SYNC_LOAD.ordinal(), "onRecGateSyncLoad");
        return true;
    }

    @Override
    public void onReady() {
        m_baseServer.onLogicReady();
    }

    @Override
    public BaseServer getServer() {
        return m_baseServer;
    }

    void onRecAllocGate(IoSession session, byte[] data) {
        Object[] gates = m_baseServer.getServerSet().getServersByType("gate");

        int minLoad = 999999999;
        int minIndex = -1;
        String uid = Strings.EMPTY;
        if (data != null) {
            try {
                InnerMsg.CustomMsg customMsg = InnerMsg.CustomMsg.parseFrom(data);
                uid = customMsg.getData().toStringUtf8();
            } catch (InvalidProtocolBufferException ignored) {

            }
        }
        Jedis jedis = getServer().getJedis();
        if (Strings.isNotBlank(uid)) {
            String index = jedis.get(uid);
            if (Strings.isBlank(index)) {
                for (int i = 0; i < gates.length; ++i) {
                    IoSession ses = m_baseServer.getServerSet().getServer(gates[i].toString());
                    int load = (int) ses.getAttribute("Load");
                    if (load < minLoad) {
                        minIndex = i;
                        minLoad = load;
                    }
                }
                jedis.set(uid, String.valueOf(minIndex));
                jedis.expire(uid, 600);
            } else {
                minIndex = Integer.parseInt(index);
            }
            logger.info("玩家登录流程 entry端口接入  {}  上次登录的索引 {} {}", uid, index, jedis.get(uid));
        } else {
            for (int i = 0; i < gates.length; ++i) {
                IoSession ses = m_baseServer.getServerSet().getServer(gates[i].toString());
                int load = (int) ses.getAttribute("Load");
                if (load < minLoad) {
                    minIndex = i;
                    minLoad = load;
                }
            }
            jedis.set(uid, String.valueOf(minIndex));
            jedis.expire(uid, 20);
        }

        if (minIndex == -1) {
            logger.info("minIndex == -1");
            session.close(true);
            return;
        }
        ServerConfig cfg = m_baseServer.getServerSet().getServerConfig(gates[minIndex].toString());
        ClientMsg.AllocGate.Builder build = ClientMsg.AllocGate.newBuilder();
        build.setAddr(cfg.frontAddr);
        build.setPort(cfg.frontPort);
        SendMessage msg = new SendMessage(ClientMsgDef.CLIENT_ADDR_RES.ordinal(), build.build().toByteArray());
        session.write(msg);
        logger.info(" 玩家登录流程 entry返回 --分配路由onRecAllocGate {}  {}  {}  {}  {}", uid, cfg.frontAddr, cfg.frontPort, data == null ? "null" : data.length, session.getAttribute(PLAYER_PROPERTY_UID));
        session.close(false);
    }

    void onRecGateSyncLoad(IoSession session, byte[] bytes) {
        InnerMsg.SyncLoad loadData = null;
        try {
            loadData = InnerMsg.SyncLoad.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        session.setAttribute("Load", loadData.getLoad());
    }

    @Override
    public boolean onSessionClosed(IoSession session) {
        int port = Integer.parseInt(session.getLocalAddress().toString().split(":")[1]);
        if (port == m_baseServer.getFrontPort()) {
            return true;
        }
        return false;
    }

    @Override
    public List<String> heartList() {
        List<String> list = new ArrayList<>();
        ServerSet serverSet = m_baseServer.getServerSet();
        Object[] servers = serverSet.getServersByType("game");
        for (Object ser : servers) {
            list.add(ser.toString());
        }
        servers = serverSet.getServersByType("back");
        for (Object ser : servers) {
            list.add(ser.toString());
        }
        servers = serverSet.getServersByType("gate");
        for (Object ser : servers) {
            list.add(ser.toString());
        }
        return list;
    }
}
