package framework;

import org.apache.mina.core.session.IoSession;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * 
 * 描述： 服务逻辑接口
 * 
 */
public interface ILogic extends PropertyKey{

	boolean onInit(BaseServer ser);

	void onReady();

	default void onStop(){
		
	}

	default void onDestroy(){
		
	}

	default void execute(){
		
	}

	BaseServer getServer();

	default void onAddClient(IoSession session){
		
	}

	default boolean onSessionClosed(IoSession session){
		return false;
	}

	default boolean onSessionIdle(IoSession session){
		return false;
	}
	
	default void initStop(){
		
	}
	
	default boolean tryToStop(){
		return true;
	}

	default void serverOffLine(String data){
		
	}
	
	default List<String> heartList(){
		return null;
	}

	default Jedis getJedis(){
		return null;
	}

	default void finalClose(){

	}
}
