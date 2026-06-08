/**   
*    
* 描述：   
* 文件：TextCmdAdapter.java
* 创建人：胡中伟
* 创建时间：2018年5月14日 下午4:36:16 
*    
*/
package framework.net;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import framework.BaseServer;
import framework.NetMessage;
import com.mysql.cj.util.StringUtils;

/**
 * 
 * 描述：
 * 
 */
public class TextCmdAdapter extends IoHandlerAdapter {
	private BaseServer m_server;
	static final Logger logger = LoggerFactory.getLogger(TextCmdAdapter.class);

	public TextCmdAdapter(BaseServer ser) {
		m_server = ser;
	}

	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
	}

	public void messageReceived(IoSession session, Object message) throws Exception {
		String str = (String) message;

		String[] cmds = str.split("\n");

		for (int i = 0; i < cmds.length; ++i) {
			NetMessage msg = new NetMessage((short) 9999, session, StringUtils.getBytes(cmds[i]));
			m_server.send(msg);
		}
	}
}
