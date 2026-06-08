package framework;

import framework.net.ClientMsgDef;
import framework.net.HttpServerHandler;
import framework.net.InnerMsgDef;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/// A thread backed actor
public class Actor extends Thread {
    static final Logger logger = LoggerFactory.getLogger(Actor.class);

    private LinkedBlockingQueue<Message> _messageQ;

    protected boolean _needShutdown;

    protected Timer _timer;

    protected Map<Integer, NetMsgHandler> _netMsgHandlerMap;
    protected Map<Integer, NetMsgHandler> _cliMsgHandlerMap;
    protected Map<Integer, NetEventHandler> _netEventHandlerMap;
    protected Map<Integer, ForwardMsgHandler> _forwardMsgHandlerMap;
    protected Map<Integer, HttpResMsgHandler> _httpResMsgHandleMap;
    protected HttpServerMsgHandler _httpServerMsgHandle;

    private static int s_index;
    private static int s_tickDuration = 10;

    public Actor() {
        this("Actor" + s_index++);
    }

    public Actor(String name) {
        this.setName(name);

        _messageQ = new LinkedBlockingQueue<>();
        _needShutdown = false;

        _netMsgHandlerMap = new HashMap<>();
        _cliMsgHandlerMap = new HashMap<>();
        _netEventHandlerMap = new HashMap<>();
        _forwardMsgHandlerMap = new HashMap<>();
        _httpResMsgHandleMap = new HashMap<>();
        _httpServerMsgHandle = null;
    }

    public void setTimer(Timer timer, int duration) {
        _timer = timer;
        s_tickDuration = duration;
    }

    public boolean isShutdown() {
        return _needShutdown;
    }

    /// Override it to do initialization logic.
    protected boolean _onInitialized() {
        return true;
    }

    protected void onDestroy() {

    }

    /// Send a message to this actor.
    /// The message will be added to actor's message queue and be executed
    /// asynchronously.
    public void send(Message message) {
        _messageQ.add(message);
    }

    /// loopCount -1 means infinite loop.
    public ActorTimer setTimer(Object listener, long miliseconds, int loopCount, String methodName, Object msgInfo) {
        ActorTimer actorTimer = null;

        miliseconds -= s_tickDuration;
        if (miliseconds < 0) {
            miliseconds = 0;
        }

        try {
            TimerHandler handler;
            try {
                handler = new TimerHandler(listener, methodName);
                actorTimer = new ActorTimer(this, miliseconds, handler, loopCount, msgInfo);
                Timeout timeout = _timer.newTimeout(actorTimer, miliseconds, TimeUnit.MILLISECONDS);
                actorTimer.setTimeout(timeout);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (SecurityException e) {
            logger.error("SetTimer failed!", e);
        }
        return actorTimer;
    }

    public void addNetMsgListener(Object listener, int msgID, String methodName) {
        try {
//			logger.info("Actor收到内网通信消息...方法{},消息id:{}",methodName,msgID);
            NetMsgHandler handler = new NetMsgHandler(listener, methodName);
            if (_netMsgHandlerMap.containsKey(msgID)) {
                logger.warn("NetMessage handler for {} already exist, the old handler will be replaced bt the new one!", msgID);
            }
            _netMsgHandlerMap.put(msgID, handler);
        } catch (NoSuchMethodException e) {
            logger.error("AddNetListener failed, method not found!", e);
        } catch (Exception e) {
            logger.error("AddNetListener failed!", e);
        }
    }

    public void addNetEventListener(Object listener, int msgID, String methodName) {
        try {
            NetEventHandler handler = new NetEventHandler(listener, methodName);

            if (_netEventHandlerMap.containsKey(msgID)) {
                logger.warn("NetMessage handler for {} already exist, the old handler will be replaced bt the new one!",
                        msgID);
            }

            _netEventHandlerMap.put(msgID, handler);

        } catch (NoSuchMethodException e) {
            logger.error("AddNetEventListener failed, method not found!", e);
        } catch (Exception e) {
            logger.error("AddNetEventListener failed!", e);
        }
    }

    public void addForwardMsgListener(Object listener, int msgID, String methodName) {
        try {
            ForwardMsgHandler handler = new ForwardMsgHandler(listener, methodName);
            if (_forwardMsgHandlerMap.containsKey(msgID)) {
                logger.warn("ForwardMessage handler for {} already exist, the old handler will be replaced bt the new one!", msgID);
            }
            _forwardMsgHandlerMap.put(msgID, handler);

        } catch (NoSuchMethodException e) {
            logger.error("AddForwardListener failed, method not found!", e);
        } catch (Exception e) {
            logger.error("AddForwardListener failed!", e);
        }
    }

    public void addClientMsgListener(Object listener, int msgID, String methodName) {
        try {
            NetMsgHandler handler = new NetMsgHandler(listener, methodName);
            if (_cliMsgHandlerMap.containsKey(msgID)) {
                logger.warn("ClientMessage handler for {} already exist, the old handler will be replaced bt the new one!", msgID);
            }
            _cliMsgHandlerMap.put(msgID, handler);
        } catch (NoSuchMethodException e) {
            logger.error("AddClientListener failed, method not found!", e);
        } catch (Exception e) {
            logger.error("AddClientListener failed!", e);
        }
    }

    public void addHttpResMsgListener(Object listener, int msgID, String methodName) {
        try {
            HttpResMsgHandler handler = new HttpResMsgHandler(listener, methodName);
            if (_httpResMsgHandleMap.containsKey(msgID)) {
                logger.warn("HttpResMsg handler for {} already exist, the old handler will be replaced bt the new one!", msgID);
            }
            _httpResMsgHandleMap.put(msgID, handler);
        } catch (NoSuchMethodException e) {
            logger.error("AddHttpResMsgListener failed, method not found!", e);
        } catch (Exception e) {
            logger.error("AddHttpResMsgListener failed!", e);
        }
    }

    public void addHttpServerMsgListener(Object listener, String methodName) {
        try {
            _httpServerMsgHandle = new HttpServerMsgHandler(listener, methodName);
        } catch (NoSuchMethodException e) {
            logger.error("AddHttpResMsgListener failed, method not found!", e);
        } catch (Exception e) {
            logger.error("AddHttpResMsgListener failed!", e);
        }
    }

    void shutdown(boolean force) {
        _messageQ.clear();
        _needShutdown = true;
        if (force) {
            this.interrupt();
        }
    }

    protected void _onMessage(Message message) throws Throwable {
        switch (message.sysType) {
            case Message.SYS_NET_MESSAGE:
                NetMessage netMessage = (NetMessage) message;
                NetMsgHandler netMsgHandler = _netMsgHandlerMap.get(netMessage.msgID);

                if (netMsgHandler == null) {
                    logger.error("[{}] No handler for NetMessage: {}", getName(), message.msgID);
                    return;
                }
                //System.out.println("SYS_NET_MESSAGE   " + InnerMsgDef.getById(netMessage.msgID).getDesc());
                netMsgHandler.handle(netMessage);
                break;
            case Message.SYS_NET_EVENT:
                NetEvent netEvent = (NetEvent) message;
                NetEventHandler netEventHandler = _netEventHandlerMap.get(netEvent.msgID);

                if (netEventHandler == null) {
                    logger.error("No handler for NetEvent: {}", message.msgID);
                    return;
                }
                //System.out.println("SYS_NET_EVENT   " + netEvent.msgID);

                netEventHandler.handle(netEvent);
                break;

            case Message.SYS_TIMER_MSG:
                TimerMsg timerMsg = (TimerMsg) message;
                timerMsg.GetActorTimer().runCallBack(timerMsg.leftCount);
                break;
            case Message.SYS_FORWARD_MSG:
                ForwardMsg forwardMsg = (ForwardMsg) message;
                ForwardMsgHandler forwardMsgHandler = _forwardMsgHandlerMap.get(forwardMsg.msgID);

                if (forwardMsgHandler == null) {
                    logger.warn("[{}] No handler for NetMessage: {}", getName(), message.msgID);
                    return;
                }
                forwardMsgHandler.handle(forwardMsg);
                System.out.println("SYS_FORWARD_MSG   " + ClientMsgDef.getById(forwardMsg.msgID).getDesc());

                break;
            case Message.SYS_CLIENT_MSG:
                NetMessage cliMessage = (NetMessage) message;
                NetMsgHandler cliMsgHandler = _cliMsgHandlerMap.get(cliMessage.msgID);
                if (cliMsgHandler == null) {
                    logger.error("[{}] No handler for NetMessage: {}", getName(), message.msgID);
                    return;
                }
                System.out.println("SYS_CLIENT_MSG   " + ClientMsgDef.getById(cliMessage.msgID).getDesc());

                cliMsgHandler.handle(cliMessage);
                break;
            case Message.SYS_HTTP_RESPONSE_MSG:
                HttpResMsg httpResMsg = (HttpResMsg) message;
                HttpResMsgHandler httpResMsgHandler = _httpResMsgHandleMap.get(httpResMsg.msgID);

                if (httpResMsgHandler == null) {
                    logger.error("[{}] No handler for NetMessage: {}", getName(), message.msgID);
                    return;
                }
                httpResMsgHandler.handle(httpResMsg);
                break;
            case Message.SYS_HTTP_SERVER_MSG:
                HttpServerMsg httpServerMsg = (HttpServerMsg) message;
                if (_httpServerMsgHandle == null) {
                    logger.error("[{}] No handler for NetMessage: {}", getName(), httpServerMsg.url);
                }
                try {
                    String json = _httpServerMsgHandle.handle(httpServerMsg);
                    writeResponse(httpServerMsg.channel, httpServerMsg.request, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            json, false);
                } catch (Exception e) {
                    writeResponse(httpServerMsg.channel, httpServerMsg.request, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            "error", true);
                    logger.error("", e);
                    throw e;
                }

            default:
                logger.error("[{}] No handler for NetMessage: {}", getName(), message.msgID);
                break;
        }
    }

    private void writeResponse(Channel channel, HttpRequest request, HttpResponseStatus status, String content,
                               boolean forceClose) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        boolean close = isClose(request);
        if (!close && !forceClose) {
            response.content().writeBytes(content.getBytes());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        }
        ChannelFuture future = channel.writeAndFlush(response);
        if (close || forceClose) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean isClose(HttpRequest request) {
        if (request.headers().contains(org.apache.http.HttpHeaders.CONNECTION, HttpServerHandler.CONNECTION_CLOSE, true)
                || (request.protocolVersion().equals(HttpVersion.HTTP_1_0)
                && !request.headers().contains(org.apache.http.HttpHeaders.CONNECTION,
                HttpServerHandler.CONNECTION_KEEP_ALIVE, true)))
            return true;
        return false;
    }

    protected void execute() {

    }

    @Override
    public void run() {
        try {
            if (!_onInitialized()) {
                logger.error("_onInitialized failed.");
                return;
            }
            BaseServer server = ((BaseServer) this);
            while (!_needShutdown) {
                if (!_messageQ.isEmpty()) {
                    List<Message> msgs = new ArrayList<>();
                    //一个玩家每s大概40条消息
                    _messageQ.drainTo(msgs);
                    for (Message message : msgs) {
                        try {
                            _onMessage(message);
                        } catch (Throwable e) {
                            String string = String.format("Error occured during handling message, actor name:%s ,sysType:%s ,msgID:%s", this.getName(), message.sysType, message.msgID);
                            logger.error(string, e);
                        }
                    }
                }
                try {
                    execute();
                    long sleepTime = _messageQ.isEmpty() ? 1 : 0;
                    if (sleepTime > 0) {
                        sleep(sleepTime);
                    }
                } catch (Throwable e) {
                    logger.error("Run Exec, actor name:{}  {}" , this.getName(), e);
                }
            }
            onDestroy();
        } catch (Throwable e) {
            logger.error("Error occcured during initilization, error:", e);
        } finally {
            logger.info("thread {} is shutdown", getName());
        }
    }
}
