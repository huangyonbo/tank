package framework.logic;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.BaseServer;
import framework.ILogic;
import framework.SystemConfigData;
import framework.back.BacKernel;
import framework.back.net.ServerSocketHandler;
import framework.back.net.TcpServerProtocolCodecFactory;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component
@Slf4j
public class BackLogic implements ILogic {
	private BaseServer m_baseServer;
	private IoAcceptor m_ioAcceptor;
	private IoHandlerAdapter m_netAdapter = null;
	private BacKernel m_kernel;

	@Override
	public boolean onInit(BaseServer ser) {
		m_baseServer = ser;
		m_kernel = new BacKernel();
		if (!m_kernel.onInit(this)) {
			log.error("BackLogic init BacKernel failed!");
			return false;
		}
		m_netAdapter = new ServerSocketHandler(this);
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), "onCustomMsg");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), "onCustomRequest");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), "onCustomResponse");

		m_ioAcceptor = new NioSocketAcceptor();
		m_ioAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TcpServerProtocolCodecFactory()));
		m_ioAcceptor.setHandler(m_netAdapter);
		m_ioAcceptor.getSessionConfig().setReadBufferSize(2048);
		m_ioAcceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
		((NioSocketAcceptor) m_ioAcceptor).setReuseAddress(true);
		try {
			int port = SystemConfigData.getConfig("listenBackSerPort", 7777);
			m_ioAcceptor.bind(new InetSocketAddress(port));
		} catch (IOException e) {
			e.printStackTrace();
			log.error("BackLogic bind port failed!");
			return false;
		}

		return true;
	}

	public BacKernel getKernel() {
		return m_kernel;
	}

	@Override
	public void execute() {
		m_kernel.execute();
	}

	@Override
	public void onReady() {
		m_baseServer.onLogicReady();
	}

	@Override
	public void onDestroy() {
		m_kernel.onDestroy();
	}
	
	@Override
	public BaseServer getServer() {
		// TODO Auto-generated method stub
		return m_baseServer;
	}


	public void onCustomMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CustomMsg custom = InnerMsg.CustomMsg.parseFrom(bytes);
		byte[] msg = null;
		if (custom.getData() != null) {
			msg = custom.getData().toByteArray();
		}
		m_kernel.onRecServerMsg((int) session.getAttribute("SerID"), custom.getMsgid(), msg);
	}

	public void onCustomRequest(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerRequest((int) session.getAttribute("SerID"), bytes);
	}

	public void onCustomResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerResponse((int) session.getAttribute("SerID"), bytes);
	}

	@Override
	public Jedis getJedis() {
		return m_baseServer.getJedis();
	}
}
