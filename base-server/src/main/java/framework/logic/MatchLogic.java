package framework.logic;

import org.apache.mina.core.session.IoSession;

import com.google.protobuf.InvalidProtocolBufferException;

import framework.BaseServer;
import framework.ILogic;
import framework.match.MatchKernel;
import framework.net.InnerMsgDef;
import framework.net.message.InnerMsg;
import org.springframework.stereotype.Component;

@Component
public class MatchLogic implements ILogic {
	private BaseServer m_baseServer;
	private MatchKernel m_kernel;

	@Override
	public boolean onInit(BaseServer ser) {
		m_baseServer = ser;
		m_kernel = new MatchKernel();
		if (!m_kernel.onInit(this)) {
			return false;
		}
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_MSG.ordinal(), "onCustomMsg");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_REQUEST.ordinal(), "onCustomRequest");
		m_baseServer.addNetMsgListener(this, InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), "onCustomResponse");
		return true;
	}

	@Override
	public void execute() {
		m_kernel.execute();
	}

	@Override
	public void onReady() {
		m_baseServer.onLogicReady();
		m_kernel.onNetReady();
	}

	@Override
	public void onDestroy() {
		
	}


	@Override
	public BaseServer getServer() {
		return m_baseServer;
	}

	void onCustomMsg(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		InnerMsg.CustomMsg custom = InnerMsg.CustomMsg.parseFrom(bytes);
		byte[] msg = null;
		if (custom.getData() != null) {
			msg = custom.getData().toByteArray();
		}
		m_kernel.onRecServerMsg((int) session.getAttribute("SerID"), custom.getMsgid(), msg);
	}

	void onCustomRequest(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerRequest((int) session.getAttribute("SerID"), bytes);
	}

	void onCustomResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerResponse((int) session.getAttribute("SerID"), bytes);
	}
	
}
