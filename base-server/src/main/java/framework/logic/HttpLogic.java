package framework.logic;

import com.google.protobuf.InvalidProtocolBufferException;
import framework.BaseServer;
import framework.ILogic;
import framework.SystemConfigData;
import framework.http.HttpKernel;
import framework.net.HttpServer;
import framework.net.InnerMsgDef;
import framework.pub.PubUtils;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

@Component
public class HttpLogic implements ILogic {
	private Logger logger = LoggerFactory.getLogger(HttpLogic.class);
	private HttpServer httpServer;
	private BaseServer m_baseServer;
	private HttpKernel m_kernel;

	/**
	 * @param ser
	 * @return
	 */
	@Override
	public boolean onInit(BaseServer ser) {
		m_baseServer = ser;
		m_kernel = new HttpKernel();
		if (!m_kernel.onInit(this)) {
			return false;
		}
		int httpServerPort = SystemConfigData.getConfig("payBackPort", 8086);
		httpServer = new HttpServer(ser);
		try {
			httpServer.start(httpServerPort);
		} catch (Exception e) {
			logger.error("", e);
			return false;
		}
		m_baseServer.addHttpServerMsgListener(this, "onHttpBack");
		m_baseServer.addNetMsgListener(this,InnerMsgDef.INNER_MSG_CUSTOM_RESPONSE.ordinal(), "onCustomResponse");
		return true;
	}

	public String onHttpBack(String url, String json) {
		return m_kernel.onRecHttpMsg(url, json);
	}

	public String getSessionFromKey(int uid) {
		String playerKey = PubUtils.getKey("player_" + uid);
		String lastGame = null;
		try {
			lastGame = getJedis().hget(playerKey, "LastGame");
		} catch (Exception e) {
			//e.printStackTrace();
		}
		if (lastGame == null || lastGame.isEmpty()) {
			Object[] games = m_baseServer.getServerSet().getServersByType("game");
			int minLoad = 999999999;
			for (int i = 0; i < games.length; ++i) {
				IoSession ses = m_baseServer.getServerSet().getServer(games[i].toString());
				int load = (int) ses.getAttribute("Load");
				if (load < minLoad) {
					minLoad = load;
					lastGame = games[i].toString();
				}
			}
		}
		return lastGame;
	}

	/**
	 * 
	 */
	@Override
	public void onReady() {
		m_baseServer.onLogicReady();
	}
	
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

	public Jedis getJedis() {
		return m_baseServer.getJedis();
	}

	public void onCustomResponse(IoSession session, byte[] bytes) throws InvalidProtocolBufferException {
		m_kernel.onServerResponse((int)session.getAttribute("SerID"), bytes);
	}
}
