package framework.net;

import com.alibaba.fastjson.JSONObject;
import framework.*;
import framework.game.Kernel;
import framework.net.message.InnerMsg;
import framework.ratelimit.RateLimiterManager;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static framework.PropertyKey.PLAYER_PROPERTY_ID;

/**
 *
 * 描述： 网络适配器 创建人：胡中伟 创建时间：2018年3月12日 下午6:16:46
 *
 */
public class NetAdapter extends IoHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(NetAdapter.class);
    private Map<Integer, BaseServer> m_servers = new HashMap<>();
    private RateLimiterManager rateLimiterManager = SpringContextUtil.getBean(RateLimiterManager.class);
    public NetAdapter() {

    }

    public synchronized void addServer(int port, BaseServer ser) {
        m_servers.put(port, ser);
    }

    public synchronized void removeServer(int port) {
        m_servers.remove(port);
    }

    public void sessionCreated(IoSession session) throws Exception {

    }

    synchronized void setSerObj(IoSession session) {
        int localPort = ((InetSocketAddress) session.getLocalAddress()).getPort();
        if (m_servers.containsKey(localPort)) {
            session.setAttribute("SerObj", m_servers.get(localPort));
        }
    }

    public void sessionOpened(IoSession session) throws Exception {
        setSerObj(session);
        if (session.getAttribute("SerObj") == null) {
            logger.info("sessionOpened: ser is null {}", session.toString());
        }
//        logger.info("sesion is open {}",session);
        BaseServer ser = (BaseServer) session.getAttribute("SerObj");
        if (ser != null) {
            int port = Integer.parseInt(session.getLocalAddress().toString().split(":")[1]);
            if (port == ser.getFrontPort()) {
                session.setAttribute("Type", "Client");
                session.setAttribute("SendIndex", new AtomicInteger());
                session.setAttribute(PropertyKey.PLAYER_PROPERTY_FRONTSER, ser.getName());
                session.setAttribute("BackSer", "");
            }
            NetEvent msg = new NetEvent(NetEvent.SESSION_OPENED, session);
            ser.send(msg);
        }
    }

    public void sessionClosed(IoSession session) throws Exception {
        BaseServer ser = (BaseServer) session.getAttribute("SerObj");
        if (ser != null) {
            NetEvent msg = new NetEvent(NetEvent.SESSION_CLOSED, session);
            ser.send(msg);
        }
        logger.info("sessionClosed {} - {}  UID {}   {}", session.getLocalAddress(), session.getRemoteAddress(),session.getAttribute(PLAYER_PROPERTY_ID),ser.getName());

    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		/*
		if ("Client".equals(session.getAttribute("Type"))) {
			NetEvent msg = new NetEvent(NetEvent.SESSION_IDLE, session);
			Send(m_server,msg);
		}
		*/
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        BaseServer ser = (BaseServer)session.getAttribute("SerObj");
        logger.error("{}   exceptionCaught: " + ser.getName(),session.getAttribute(PLAYER_PROPERTY_ID),cause);
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
        JSONObject jsonObject = new JSONObject();
        try {
            IoBuffer buffer = (IoBuffer) message;
            int dataLength = buffer.getInt();
            BaseServer ser = (BaseServer) session.getAttribute("SerObj");
            Integer uid = (Integer) session.getAttribute("Uid");
            if (uid != null && uid != 0) {
                if (!rateLimiterManager.tryAcquire(uid)) {
                    InnerMsg.String.Builder builder = InnerMsg.String.newBuilder();
                    builder.setValue(String.valueOf(uid));
                    byte[] data = builder.build().toByteArray();
                    ser.sendMsgToServer(ServerSet.SERVER_LOGIC_NAME_GAME, InnerMsgDef.INNER_MSG_ACCOUNT_STATUS.ordinal(), data);

                    InnerMsg.KickPlayer.Builder build = InnerMsg.KickPlayer.newBuilder();
                    build.setUid(uid);
                    build.setCode(Kernel.KickType.KICK.ordinal());
                    ser.sendMsgToServer(ser.getName(), InnerMsgDef.INNER_MSG_KICK_PLAYER.ordinal(), build.build().toByteArray());
                    logger.error("too many message requests kickout uid : {}" ,uid);
                    return;
                }
            }
            if ("Client".equals(session.getAttribute("Type"))) {
                jsonObject.put("session", session.toString());
                int recvIndex = buffer.getInt();
                dataLength -= 4;
                short msgID = 0;
                if (session.getAttribute("VerifyCode") != null) {
                    short code = (short) session.getAttribute("VerifyCode");
                    byte[] byteContent = new byte[dataLength];
                    buffer.get(byteContent);
                    byte[] result = Verify.decode(recvIndex, code, byteContent, byteContent.length);
                    IoBuffer buffer1 = IoBuffer.wrap(result);
                    msgID = buffer1.getShort();
                    int pbLen = result.length - 2;
                    byte[] contentBytes = null;
                    if (pbLen > 0) {
                        contentBytes = new byte[pbLen];
                        buffer1.get(contentBytes);
                    }
                    buffer1.clear();
                    jsonObject.put("verifyMsgid", "msgID");
                    jsonObject.put("msgID", msgID);
                    jsonObject.put("can", true);
                    if (ser != null) {
                        NetMessage msg = new NetMessage(NetMessage.SYS_CLIENT_MSG, msgID, session, contentBytes);
                        ser.send(msg);
                    }
                } else {
                    jsonObject.put("NotverifyMsgid", "msgID");
                    jsonObject.put("msgID", msgID);
                    jsonObject.put("can", true);
                    msgID = buffer.getShort();
                    int pbLen = dataLength - 6;
                    byte[] contentBytes = null;
                    if (pbLen > 0) {
                        contentBytes = new byte[pbLen];
                        buffer.get(contentBytes);
                    }
                    if (ser != null) {
                        NetMessage msg = new NetMessage(NetMessage.SYS_CLIENT_MSG, msgID, session, contentBytes);
                        ser.send(msg);
                    }
                }
                buffer.clear();
//                logger.info("Received ClientMsg len={},msgId={}",dataLength,msgID);
                return;
            }
            short msgID = buffer.getShort();
            int pbLen = dataLength - 2;
            byte[] contentBytes = null;
            if (pbLen > 0) {
                contentBytes = new byte[pbLen];
                buffer.get(contentBytes);
            }
            jsonObject.put("NotClient", "");
            jsonObject.put("msgID", msgID);
            jsonObject.put("can", true);
            buffer.clear();
            if (ser != null) {
                NetMessage msg = new NetMessage(msgID, session, contentBytes);
                ser.send(msg);
            }
        } finally {
//            if (jsonObject.getInteger("msgID")==0) {
//                logger.info(jsonObject.toString());
//            }
        }
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        //logger.debug("messageSent thread is: {}", Thread.currentThread().getName());
    }
}
