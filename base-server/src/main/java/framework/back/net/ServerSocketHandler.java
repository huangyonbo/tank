package framework.back.net;

import framework.back.BacKernel;
import framework.logic.BackLogic;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSocketHandler extends IoHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ServerSocketHandler.class.getName());

	BackLogic m_BackLogic;
	BacKernel m_kernel;
	public ServerSocketHandler(BackLogic logic)
	{
		m_BackLogic = logic;
		m_kernel = m_BackLogic.getKernel();
	}
	
	@Override
	public void sessionIdle(IoSession session, IdleStatus arg) throws Exception {
		//session.close(true);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable arg) throws Exception {
		session.close(true);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		Map<String, Object> map = analysisAmfMessage(session, message);
		dealAmfMessage(session, map);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
	}

	protected void dealAmfMessage(IoSession session, Map<String, Object> map) {
		try {
			String url = (String) map.get("method");
			Object[] args = (Object[]) map.get("args");
			long msgIndex = (long)map.get("msgIndex");
			logger.info("Cluster Receive Request From Client, Method: " + url + " | Args: " + Arrays.deepToString(args));
			String[] urls = url.split("/");
			String module = urls[0];
			String method = urls[1];
			IDataCallBack cb = (Object result)->{
				sendData(msgIndex,session, url, result != null ? result : 0);
			};
			Object[] args_login = new Object[args.length + 2];
			args_login[0] = m_kernel;
			for (int i = 0; i < args.length; i++) {
				args_login[i+1] = args[i];
			}
			args_login[args.length + 1] = cb;
			args = args_login;

			Object service = m_kernel.getModule(module);
			Class<?>[] argClasses = getClasses(args);
			argClasses[args.length - 1] = IDataCallBack.class;
			Method m = service.getClass().getDeclaredMethod(method, argClasses);
			m.invoke(service, args);
		} catch (Exception e) {
			logger.error("SocketHandler dealAmfMessage error:" + StackTraceUtil.getStackTrace(e));
		}
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> analysisAmfMessage(IoSession session, Object message) {
		ObjectInputStream objectInputStream = null;
		try {
			IoBuffer buffer = (IoBuffer) message;
			byte[] contentBytes = new byte[buffer.getInt()];
			buffer.get(contentBytes);

			objectInputStream = new ObjectInputStream(new ByteArrayInputStream(contentBytes));
			Map<String, Object> map = (Map<String, Object>)objectInputStream.readObject();
			return map;
		} catch (Exception e) {
			logger.error("SocketHandler analysisAmfMessage error:" + StackTraceUtil.getStackTrace(e));
		}finally {
			try {
				if (objectInputStream != null)
					objectInputStream.close();
			} catch (IOException e) {
				logger.error(StackTraceUtil.getStackTrace(e));
			}
		}
		return null;
	}

	protected Class<?>[] getClasses(Object[] args) {
		try {
			Class<?>[] classArray = new Class<?>[args.length];

			for (int i = 0; i < args.length; i++) {
				classArray[i] = args[i].getClass();
				if (args[i] instanceof Integer) {
					classArray[i] = Integer.TYPE;
					continue;
				} else if (args[i] instanceof Boolean) {
					classArray[i] = Boolean.TYPE;
					continue;
				} else if (args[i] instanceof Map) {
					classArray[i] = Map.class;
					continue;
				} else if (args[i] instanceof Long) {
					classArray[i] = Long.TYPE;
					continue;
				} else if (args[i] instanceof Double) {
					classArray[i] = Double.TYPE;
					continue;
				} else if (args[i] instanceof IoSession) {
					classArray[i] = IoSession.class;
					continue;
				} else if (args[i] instanceof Float) {
					classArray[i] = Float.TYPE;
				}else if (args[i] instanceof List){
					classArray[i] = List.class;
				}
			}
			return classArray;
		} catch (Exception e) {
			logger.error("SocketHandler getClasses error:" + StackTraceUtil.getStackTrace(e));
		}
		return null;
	}

	private void sendData(long msgIndex,IoSession session, String method, Object objects){
		ByteArrayOutputStream bout = null;
		ObjectOutputStream objectOutputStream = null;
		try {
			Map<String, Object> sendMap = new HashMap<>();
			sendMap.put("msgId", msgIndex);
			sendMap.put("body", objects);
			bout = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(bout);
			objectOutputStream.writeObject(sendMap);
			objectOutputStream.flush();
			byte[] content_out = bout.toByteArray();
			IoBuffer bb = IoBuffer.allocate(content_out.length + 4);
			bb.putInt(content_out.length);
			bb.put(content_out);
			bb.flip();
			session.write(bb);
			logger.info("Cluster Send Data To Client, Data: " + objects);
		} catch (IOException e) {
			logger.error(StackTraceUtil.getStackTrace(e));
		}finally {
			try {
				if (objectOutputStream != null){
					objectOutputStream.close();
				}
				if (bout != null){
					bout.close();
				}
			} catch (Exception e) {
				logger.error(StackTraceUtil.getStackTrace(e));
			}
		}
	}
}
