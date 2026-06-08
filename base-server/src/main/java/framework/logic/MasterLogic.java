package framework.logic;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.BaseServer;
import framework.ILogic;
import framework.SystemConfigData;
import framework.master.MasterKernel;
import framework.net.InnerMsgDef;
import framework.net.TextCmdAdapter;
import framework.net.TextCmdCodecFactory;
import framework.net.message.InnerMsg;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component
public class MasterLogic implements ILogic {
	BaseServer m_baseServer;
	private MasterKernel m_kernel;
	private IoAcceptor m_ioAcceptor;
	private IoHandlerAdapter m_netAdapter = null;
	
	/**
	 * @param ser
	 * @return
	 */
	@Override
	public boolean onInit(BaseServer ser) {
		m_baseServer = ser;
		m_kernel = new MasterKernel();
		if (!m_kernel.onInit(this)) {
			return false;
		}
		m_netAdapter = new TextCmdAdapter(ser);
		m_ioAcceptor = new NioSocketAcceptor();
		m_ioAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextCmdCodecFactory()));
		m_ioAcceptor.setHandler(m_netAdapter);
		m_ioAcceptor.getSessionConfig().setReadBufferSize(2048);
		((NioSocketAcceptor) m_ioAcceptor).setReuseAddress(true);
		m_ioAcceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
		try {
			int port  = SystemConfigData.getConfig("masterPort",10017);
			m_ioAcceptor.bind(new InetSocketAddress(port));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), "onCustomMsg");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), "onCustomRequest");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), "onCustomResponse");
		m_baseServer.addNetMsgListener(this, 9999, "onRecCmd");
		return true;
	}

	@Override
	public void execute() {
		m_kernel.execute();
	}

	void onRecCmd(IoSession session, byte[] bytes) {
		String cmd = StringUtils.newStringUtf8(bytes);

		String[] cmds = cmd.split(" ");
		m_kernel.onCmd(session, cmds);
	}

	/**
	 * 
	 */
	@Override
	public void onReady() {
		m_baseServer.onLogicReady();
	}
	
	/**
	 * 
	 */
	@Override
	public void onDestroy() {
		m_kernel.onDestroy();
	}


	/**
	 * @return
	 */
	@Override
	public BaseServer getServer() {
		return m_baseServer;
	}


	void onCustomMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CustomMsg custom = InnerMsg.CustomMsg.parseFrom(bytes);
		m_kernel.onRecServerMsg((int) session.getAttribute("SerID"), custom.getMsgid(),
				custom.getData().toByteArray());
	}

	void onCustomRequest(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerRequest((int) session.getAttribute("SerID"), bytes);
	}

	void onCustomResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerResponse((int) session.getAttribute("SerID"), bytes);
	}
}
