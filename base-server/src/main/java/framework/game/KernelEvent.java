package framework.game;

/**
 * 
 * 描述： 内核事件枚举
 * 
 */
public enum KernelEvent {
	/**
	 * 类创建 回调格式：void func(IKernel kernel, String script)
	 */
	KEVENT_ON_CREATE_CLASS,

	KEVENT_ON_CLASS_READY,

	/**
	 * 创建游戏对象 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_ON_CREATE,

	/**
	 * 加载游戏对象 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_ON_LOAD,

	/**
	 * 玩家上线 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_ON_LINE,

	/**
	 * 玩家下线 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_OFF_LINE,

	/**
	 * 存储游戏对象 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_ON_STORE,

	/**
	 * 销毁游戏对象 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_ON_DESTROY,

	/**
	 * 玩家重新连接成功 回调格式：void func(IKernel kernel, IGameObject self)
	 */
	KEVENT_ON_RECONNECT,

	/**
	 * 进入对象 回调格式：void func(IKernel kernel, IGameObject self, IGameObject target)
	 */
	KEVENT_ON_ENTER,

	/**
	 * 离开对象 回调格式：void func(IKernel kernel, IGameObject self, IGameObject target)
	 */
	KEVENT_ON_LEAVE,

	/**
	 * 玩家坐下 回调格式：void func(IKernel kernel, IGameObject self, IGameObject target)
	 */
	KEVENT_ON_SITDOWN,

	/**
	 * 玩家起立 回调格式：void func(IKernel kernel, IGameObject self, IGameObject target)
	 */
	KEVENT_ON_STANDUP,

	/**
	 * 离线数据 回调格式：void func(IKernel kernel, IGameObject self, int type, String
	 * context, String reason)
	 */
	KEVENT_ON_OFFLINEDATA,

	/**
	 * 配置更新 回调格式：void func(IKernel kernel, String cfg)
	 */
	KEVENT_ON_UPDATE_CFG,
}
