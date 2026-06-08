package framework.game;

import com.esotericsoftware.reflectasm.MethodAccess;
import framework.MethodAccessCache;
import framework.MethodCallBackData;
import framework.PropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * 描述： 逻辑类集合
 *
 */
public class ClassSet implements PropertyKey{

	private final static Logger logger = LoggerFactory.getLogger(ClassSet.class);

	private final static long HIGHER_VALUE = 0xFFL;
	private final static long MIDDLE_VALUE = 0xFFFFFFFFL;
	private final static long LOWER_VALUE  = 0xFFFFFFL;

	Kernel m_kernel;

	static class ClassData {
		String name;
		String parent;
		GameObjectType type;
		GameObject template;
		boolean inited;

		Map<KernelEvent, Set<MethodCallBackData>> mapEvents = new HashMap<>();
		Map<String, Set<MethodCallBackData>> mapProperty = new HashMap<>();
		Map<String, Set<MethodCallBackData>> mapRecord = new HashMap<>();
		Map<Integer, Set<MethodCallBackData>> mapCommand = new HashMap<>();

		Map<String, Set<MethodCallBackData>> listenSetPropertyMap = new HashMap<>();
	}

	Map<String, ClassData> mapClasses = new HashMap<>();
	List<GameObject> allObjs = new ArrayList<>();
	LinkedList<Integer> unusedIndex = new LinkedList<>();
	long tailIndex = 0;

	public ClassSet(Kernel kernel) {
		m_kernel = kernel;
		for (int i = 0; i < 4096; ++i) {
			allObjs.add(null);
			unusedIndex.addLast(i);
		}
	}

	public void initRootClass(String name, GameObjectType type) {
		ClassData data = new ClassData();
		data.name   = name;
		data.parent = "";
		data.type = type;
		data.template = createGameObject(type);
		data.template.m_root = true;
		data.template.setScript(name);
		data.inited = false;
		mapClasses.put(name, data);
	}

	public boolean addClass(String name, String parent) {
		if (!mapClasses.containsKey(parent)) {
			return false;
		}

		if (mapClasses.containsKey(name)) {
			return false;
		}
		ClassData parentData = mapClasses.get(parent);
		ClassData data = new ClassData();
		data.name = name;
		data.parent = parent;
		data.type = parentData.type;
		data.template = createGameObject(data.type);
		data.template.setScript(name);
		data.inited = false;

		mapClasses.put(name, data);

		return true;
	}

	private void processInherit(ClassData data) {
		if (!data.inited) {
			data.template.initInnerData();

			ClassData parent = mapClasses.get(data.parent);
			if (parent != null) {
				processInherit(parent);
				data.template.copyFrom(parent.template);
			}

			data.inited = true;
		}
	}

	public void createClasses() {
		for (Entry<String, ClassData> entry : mapClasses.entrySet()) {
			runEvent(KernelEvent.KEVENT_ON_CREATE_CLASS, entry.getKey(), entry.getKey());
		}
		for (Entry<String, ClassData> entry : mapClasses.entrySet()) {
			processInherit(entry.getValue());
		}
		for (Entry<String, ClassData> entry : mapClasses.entrySet()) {
			runEvent(KernelEvent.KEVENT_ON_CLASS_READY, entry.getKey(), entry.getKey());
		}
	}

	public boolean regEvent(KernelEvent event, String script, Object listener, String methodName) {
		if (!mapClasses.containsKey(script)) {
			return false;
		}

		ClassData data = mapClasses.get(script);
		MethodCallBackData cbData = new MethodCallBackData();
		cbData.listener = listener;
		// cbData.access = MethodAccess.get(listener.getClass());
		cbData.access = MethodAccessCache.tryToGet(listener.getClass());
		cbData.methodIndex = getEventCBMethod(cbData.access, methodName, event);

		if (!data.mapEvents.containsKey(event)) {
			Set<MethodCallBackData> temp = new HashSet<>();
			temp.add(cbData);
			data.mapEvents.put(event, temp);
		} else {
			data.mapEvents.get(event).add(cbData);
		}
		return true;
	}

	public void runEvent(KernelEvent event, String script, Object... args) {
		if (!mapClasses.containsKey(script)) {
			return;
		}

		ClassData data = mapClasses.get(script);
		// run parent event
		runEvent(event, data.parent, args);

		if (!data.mapEvents.containsKey(event)) {
			return;
		}

		long start = m_kernel.getServerTime();
		for (MethodCallBackData cb : data.mapEvents.get(event)) {
			long doTime = m_kernel.getServerTime();
			dispatchEvent(event, cb, args);
			long usedo = m_kernel.getServerTime() - doTime;
			if (usedo > 30) {
				logger.warn("runEvent [{}] for [{}] used time:{} {} {} {} {}", event, script, usedo,cb.listener.getClass().getName(),cb.access.getMethodNames()[cb.methodIndex]);
			}
		}
		long used = m_kernel.getServerTime() - start;
		if (used > 100) {
			logger.warn("runEvent [{}] for [{}] used time:{}", event, script, used);
		}
	}

	private void dispatchEvent(KernelEvent event, MethodCallBackData cb, Object... args) {
		try {
			switch (event) {
				case KEVENT_ON_CREATE_CLASS:
				case KEVENT_ON_CLASS_READY:
				case KEVENT_ON_CREATE:
				case KEVENT_ON_LOAD:
				case KEVENT_ON_LINE:
				case KEVENT_OFF_LINE:
				case KEVENT_ON_STORE:
				case KEVENT_ON_DESTROY:
				case KEVENT_ON_RECONNECT:
				case KEVENT_ON_UPDATE_CFG:
					cb.access.invoke(cb.listener, cb.methodIndex, m_kernel,args[0]);
					break;
				case KEVENT_ON_LEAVE:
				case KEVENT_ON_ENTER:
				case KEVENT_ON_SITDOWN:
				case KEVENT_ON_STANDUP:
					cb.access.invoke(cb.listener, cb.methodIndex, m_kernel,args[0],args[1]);
					break;
				case KEVENT_ON_OFFLINEDATA:
					cb.access.invoke(cb.listener, cb.methodIndex, m_kernel,args[0],args[1], args[2],args[3]);
					break;
			}
		} catch (Throwable e) {
			logger.error("dispatchEvent error : " + cb.info());
			e.printStackTrace();
		}
	}

	private int getEventCBMethod(MethodAccess access, String methodName, KernelEvent event) {
		try {
			switch (event) {
				case KEVENT_ON_CREATE_CLASS:
				case KEVENT_ON_CLASS_READY:
				case KEVENT_ON_UPDATE_CFG:
					return access.getIndex(methodName, IKernel.class, String.class);
				case KEVENT_ON_CREATE:
				case KEVENT_ON_LOAD:
				case KEVENT_ON_LINE:
				case KEVENT_OFF_LINE:
				case KEVENT_ON_STORE:
				case KEVENT_ON_DESTROY:
				case KEVENT_ON_RECONNECT:
					return access.getIndex(methodName, IKernel.class, IGameObject.class);
				case KEVENT_ON_LEAVE:
				case KEVENT_ON_ENTER:
				case KEVENT_ON_SITDOWN:
				case KEVENT_ON_STANDUP:
					return access.getIndex(methodName, IKernel.class, IGameObject.class, IGameObject.class);
				case KEVENT_ON_OFFLINEDATA:
					return access.getIndex(methodName, IKernel.class, IGameObject.class, int.class, String.class, String.class);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void regCommand(String script, int cmdid, Object listener, String methodName) {
		ClassData data = mapClasses.get(script);
		if (data == null) {
			return;
		}

		MethodCallBackData cbdata = new MethodCallBackData();
		cbdata.listener = listener;
		// cbdata.access = MethodAccess.get(listener.getClass());
		cbdata.access = MethodAccessCache.tryToGet(listener.getClass());
		cbdata.methodIndex = cbdata.access.getIndex(methodName);

		if (!data.mapCommand.containsKey(cmdid)) {
			Set<MethodCallBackData> temp = new HashSet<>();
			temp.add(cbdata);
			data.mapCommand.put(cmdid, temp);
		} else {
			data.mapCommand.get(cmdid).add(cbdata);
		}
	}

	public void onCommand(String script, IGameObject object, int cmdid, Object... args) {
		ClassData data = mapClasses.get(script);
		if (data == null) {
			return;
		}
		onCommand(data.parent, object, cmdid, args);
		if (!data.mapCommand.containsKey(cmdid)) {
			return;
		}
		long start = m_kernel.getServerTime();
		for (MethodCallBackData cb : data.mapCommand.get(cmdid)) {
			try {
				cb.access.invoke(cb.listener, cb.methodIndex, m_kernel, object, args);
			} catch (Exception e) {
				logger.error("onCommand error : " + cb.info());
			}
		}
		long used = m_kernel.getServerTime() - start;
		if (used > 100) {
			logger.warn("onCommand [{}] for [{}-{}] used time:{}", cmdid, object.getString(PLAYER_PROPERTY_NAME), script, used);
		}
	}

	public static int getObjectSerID(long objectId) {
		return (int) ((objectId >> 56) & HIGHER_VALUE);
	}

	public static int getObjectIndex(long objectId) {
		return (int) ((objectId >> 24) & MIDDLE_VALUE);
	}

	public long allocObjectID(long index) {
		long serId = m_kernel.getSerID();
		tailIndex ++;
		return ((serId & HIGHER_VALUE) << 56) | ((index & MIDDLE_VALUE) << 24) | (tailIndex & LOWER_VALUE);
	}

	public GameObject createGameObject(GameObjectType type) {
		GameObject obj = null;
		switch (type) {
			case GOTYPE_PLAYER:
				obj = new GamePlayer(m_kernel);
				break;
			case GOTYPE_ITEM:
				obj = new GameItem(m_kernel);
				break;
			case GOTYPE_ROOM:
				obj = new GameRoom(m_kernel);
				break;
			case GOTYPE_DESK:
				obj = new GameDesk(m_kernel);
				break;
			case GOTYPE_NPC:
				obj = new GameNpc(m_kernel);
				break;
			case GOTYPE_HELP:
				obj = new GameHelp(m_kernel);
				break;
			case GOTYPE_CONTAINER:
				obj = new GameContainer(m_kernel);
			default:
				break;
		}
		if (obj == null) {
			return null;
		}
		int index;
		if (unusedIndex.isEmpty()) {
			index = allObjs.size();
			allObjs.add(obj);
		} else {
			index = unusedIndex.removeFirst();
			allObjs.set(index,obj);
		}
		obj.setObjectID(allocObjectID(index));
		return obj;
	}

	public GameObject createWorld() {
		return createObjectByScript("World");
	}

	GameObject getGameObject(long objectId) {
		int serId = getObjectSerID(objectId);
		int index = getObjectIndex(objectId);
		if (serId != m_kernel.getSerID() || index < 0 || index >= allObjs.size()) {
			return null;
		}
		GameObject obj = allObjs.get(index);
		if (obj != null && obj.getObjectID() == objectId) {
			return obj;
		}
		return null;
	}

	public void destroyGameObject(IGameObject obj) {
		try {
			obj.onDestroy();
		} catch (Exception e) {
			logger.error("destroyGameObject error",e);
		}
		long objectId = obj.getObjectID();
		int serId = getObjectSerID(objectId);
		int index = getObjectIndex(objectId);
		if (serId != m_kernel.getSerID() || index < 0 || index >= allObjs.size()) {
			logger.error("destroyGameObject error serId={} index={}",serId,index);
			return;
		}
		if (allObjs.get(index) != obj) {
			return;
		}
		allObjs.set(index,null);
		unusedIndex.add(index);
	}

	public GameObject createObjectByScript(String script) {
		if (!mapClasses.containsKey(script)) {
			logger.error("script [{}] not exist", script);
			return null;
		}

		ClassData data = mapClasses.get(script);
		GameObject obj = createGameObject(data.type);
		if (obj == null) {
			logger.error("obj == null with type {}", data.type);
			return null;
		}
		obj.setScript(script);
		obj.copyFrom(data.template);
		obj.innerInit();

		obj.onCreate();

		return obj;
	}

	public GameObject getTemplate(String script) {
		if (!mapClasses.containsKey(script)) {
			return null;
		}
		return mapClasses.get(script).template;
	}

	public boolean listenPropertyChange(String name, String script, Object listener, String methodName) {
		if (!mapClasses.containsKey(script)) {
			return false;
		}

		ClassData data = mapClasses.get(script);

		MethodCallBackData cbdata = new MethodCallBackData();
		cbdata.listener = listener;
		cbdata.access = MethodAccessCache.tryToGet(listener.getClass());
		cbdata.methodIndex = cbdata.access.getIndex(methodName, IKernel.class, IGameObject.class, String.class, Object.class);
		if (!data.mapProperty.containsKey(name)) {
			Set<MethodCallBackData> temp = new HashSet<>();
			temp.add(cbdata);
			data.mapProperty.put(name, temp);
		} else {
			data.mapProperty.get(name).add(cbdata);
		}
		return true;
	}

	public void listenSetProperty(String name, String script, Object listener, String methodName) {
		if (!mapClasses.containsKey(script)) {
			return;
		}

		ClassData data = mapClasses.get(script);

		MethodCallBackData cbdata = new MethodCallBackData();
		cbdata.listener = listener;
		cbdata.access = MethodAccessCache.tryToGet(listener.getClass());
		cbdata.methodIndex = cbdata.access.getIndex(methodName, IKernel.class, IGameObject.class, String.class, Object.class);
		if (!data.listenSetPropertyMap.containsKey(name)) {
			Set<MethodCallBackData> temp = new HashSet<>();
			temp.add(cbdata);
			data.listenSetPropertyMap.put(name, temp);
		} else {
			data.listenSetPropertyMap.get(name).add(cbdata);
		}
	}

	public void onSetProperty(IGameObject object, String name, Object value) {
		String script = object.getScript();
		ClassData data = mapClasses.get(script);
		if (data == null) {
			return;
		}
		Set<MethodCallBackData> callBackData = data.listenSetPropertyMap.get(name);
		if (callBackData == null) {
			return;
		}
		long start = m_kernel.getServerTime();
		for (MethodCallBackData cb : callBackData) {
			cb.access.invoke(cb.listener, cb.methodIndex, m_kernel, object, name, value);
		}
		long used = m_kernel.getServerTime() - start;
		if (used > 100) {
			logger.warn("onSetProperty [{}] for [{}-{}] used time:{}", name, object.getString(PLAYER_PROPERTY_NAME), script, used);
		}
	}

	public void onPropertyChange(IGameObject obj, String name, Object oldv) {
		String script = obj.getScript();
		ClassData data = mapClasses.get(script);
		if (data == null) {
			return;
		}
		Set<MethodCallBackData> callBackData = data.mapProperty.get(name);
		if (callBackData == null){
			return;
		}
		long start = m_kernel.getServerTime();
		for (MethodCallBackData cb : callBackData) {
			cb.access.invoke(cb.listener, cb.methodIndex, m_kernel, obj, name, oldv);
		}
		long used = m_kernel.getServerTime() - start;
		if (used > 100) {
			logger.warn("onPropertyChange [{}] for [{}-{}] used time:{}", name, obj.getString(PLAYER_PROPERTY_NAME), script, used);
		}
	}

	public void listenRecordChange(String name, String script, Object listener, String methodName) {
		if (!mapClasses.containsKey(script)) {
			return;
		}
		ClassData data = mapClasses.get(script);
		MethodCallBackData cbdata = new MethodCallBackData();
		cbdata.listener = listener;
		cbdata.access = MethodAccessCache.tryToGet(listener.getClass());
		cbdata.methodIndex = cbdata.access.getIndex(methodName, IKernel.class, IGameObject.class, String.class);
		if (!data.mapRecord.containsKey(name)) {
			Set<MethodCallBackData> temp = new HashSet<>();
			temp.add(cbdata);
			data.mapRecord.put(name, temp);
		} else {
			data.mapRecord.get(name).add(cbdata);
		}
	}

	public void onRecordChange(IGameObject obj, String name) {
		String script = obj.getScript();
		ClassData data = mapClasses.get(script);
		if (data == null) {
			return;
		}
		Set<MethodCallBackData>  callBackData = data.mapRecord.get(name);
		if (callBackData == null) {
			return;
		}
		long start = m_kernel.getServerTime();
		for (MethodCallBackData cb : callBackData) {
			cb.access.invoke(cb.listener, cb.methodIndex, m_kernel, obj, name);
		}
		long used = m_kernel.getServerTime() - start;
		if (used > 100) {
			logger.warn("onRecordChange [{}] for [{}-{}] used time:{}", name, obj.getString(PLAYER_PROPERTY_NAME), script, used);
		}
	}
}
