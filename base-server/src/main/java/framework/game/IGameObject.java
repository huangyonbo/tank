package framework.game;

import com.alibaba.fastjson.JSONObject;
import framework.PropertyKey;
import framework.game.IKernel.PlayerState;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;

public interface IGameObject extends PropertyKey {
	/** 获取父对象
	 * @return 父对象，没有则为null
	 */
	IGameObject getParent();

	/** 获取脚本
	 * @return 脚本名
	 */
	String getScript();

	/** 获取类型
	 * @return 对象类型
	 */
	GameObjectType getType();

	/** 获取对象号
	 * @return 对象号
	 */
	long getObjectID();

	/** 获取位置
	 * @return 位置
	 */
	int getPos();

	/** 获取容量
	 * @return 总容量
	 */
	int getCapacity();

	/** 获取表格
	 * @param name 表名
	 * @return 表格对象，没有则为null
	 */
	IRecord getRecord(String name);

	/** 增加子对象
	 * @param pos 位置
	 * @param child 子对象
	 * @return 是否成功
	 */
	boolean addChild(int pos, IGameObject child);

	/** 增加子对象
	 * @param child 子对象
	 * @return 位置，失败返回-1
	 */
	int addChild(IGameObject child);

	/** 获取子对象
	 * @param index 位置
	 * @return 对象，没有返回null
	 */
	IGameObject getChild(int index);

	/** 获取容器
	 * @param name 容器名
	 * @return 容器对象
	 */
	IGameObject getContainer(String name);

	/** 获取所有同脚本的子对象
	 * @param script 脚本名
	 * @param childs 返回容器
	 * @return 子对象数量
	 */
	int getChildsByScript(String script, List<Long> childs);

	/** 获取所有同类型的子对象
	 * @param type 类型
	 * @param childs 返回容器
	 * @return 子对象数量
	 */
	int getChildsByType(GameObjectType type, List<Long> childs);

	/** 获取所有同id的子对象
	 * @param id 配置id
	 * @param childs 返回容器
	 * @return 子对象数量
	 */
	int getChildsById(String id, List<Long> childs);

	/**	根据ID查找子对象位置
	 * @param id 配置id
	 * @return 所在位置，未找到返回-1
	 */
	int findChildById(int pos, String id);

	/**
	 * 根据ID查找子对象
	 * @param id 道具id
	 * @return item对象
	 */
	List<IGameObject> findChildObjById(String id);

	/** 删除子对象
	 * @param child 对象
	 */
	void removeChild(IGameObject child);

	/** 删除子对象
	 * @param pos 位置
	 */
	void removeChild(int pos);

	/** 设置属性
	 * @param key 属性名
	 * @param value 属性值
	 */
	void setProperty(String key, Object value);

	/** 设置属性
	 * @param key 属性名
	 * @param value 属性值
	 * @param runChange 是否触发属性变化事件
	 *
	 */
	void setProperty(String key, Object value,boolean runChange);

	/**
	 * 系统设置属性
	 * @param key
	 * @param value
	 * @param system
	 * @param reason
	 */
	default void setProperty(String key, Object value, int system, String reason){
		//这里只有玩家对象才会添加玩家日志，其他对象都直接设置
		setProperty(key,value);
	};

	/** 设置属性，并立刻同步给客户端
	 * @param key 属性名
	 * @param value 属性值
	 */
	void setPropertyImmediately(String key, Object value);

	/** 获取属性
	 * @param name 属性名
	 * @return 属性值
	 */
	Object getProperty(String name);
	ValueType getProType(String name);
	short getShort(String name);
	boolean getBool(String name);
	int getInt(String name);
	long getLong(String name);
	float getFloat(String name);
	double getDouble(String name);
	String getString(String name);

	void addTempData(String key, ValueType type, Object value);
	boolean haveTempData(String key);
	void setTempData(String key, Object value);
	void removeTempData(String key);
	Object getTempData(String key);
	short getTempShort(String key);
	boolean getTempBool(String key);
	int getTempInt(String key);
	long getTempLong(String key);
	float getTempFloat(String key);
	double getTempDouble(String key);
	String getTempString(String key);
	ValueType getTempType(String key);

	/** 检测心跳
	 * @param name 心跳名
	 * @return 是否存在
	 */
	boolean haveHeartBeat(String name);

	/** 删除心跳
	 * @param name 心跳名
	 */
	void removeHeartBeat(String name);

	/** 获取座位数
	 * @return 座位数量
	 */
	int getSeatCount();

	/** 获取空闲的座位数
	 * @return 空闲数量
	 */
	int getFreeSeatCount();

	/** 获取座位上的对象
	 * @param seatid 座位号
	 * @return 对象，没有则返回null
	 */
	IGameObject getSeatObject(int seatid);

	/**增加视图
	 * @param viewid 视图id
	 * @param name 容器名
	 * @return 是否成功
	 */
	boolean addViewport(int viewid, String name);

	/**移除视图
	 * @param viewid 视图id
	 */
	void removeViewport(int viewid);

	/** 获取剩余时效
	 * @return -1：长期有效，0：已到期，>0 时效
	 */
	int getLeftTime();

	PlayerState getState();

	default void onDestroy() {

	}
	default String getRecordMeta(String name){
		IRecord record=this.getRecord(name);
		JSONObject jsonObject = new JSONObject();
		for (int i = 0; i < record.getRows(); i++) {
			StringBuilder stringBuilder = new StringBuilder();
			for (int j = 0; j < record.getCols(); j++) {
				stringBuilder.append(record.getValue(i,j)+" ");
			}
			jsonObject.put(i+"",stringBuilder.toString());
		}
		return jsonObject.toString();
	}

	default String getRecordMetaRow(String name,int row){
		IRecord record=this.getRecord(name);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{"+row+":{");
		for (int j = 0; j < record.getCols(); j++) {
			stringBuilder.append(record.getValue(row,j)+",");
		}
		stringBuilder.append("}}");
		return stringBuilder.toString();
	}

	default String getRecordMeta(IRecord record){
		StringJoiner stringJoiner=new StringJoiner("\r\n");
		StringJoiner sj=null;
		for (int i = 0; i < record.getRows(); i++) {
			sj=new StringJoiner(" ");
			for (int j = 0; j < record.getCols(); j++) {//改行每列数据
				sj.add(record.getValue(i,j)==null? StringUtils.EMPTY:record.getValue(i,j).toString());
			}
			stringJoiner.add(i+":"+sj.toString());
		}
		return "\r\n"+stringJoiner.toString();
	}
}
