package framework.game;

/**
 * 
 * 描述： 道具对象
 * 
 */
class GameItem extends GameObject {
	
	public GameItem(Kernel kernel) {
		super(kernel);
		m_Type = GameObjectType.GOTYPE_ITEM;
	}

	public void initInnerData() {
		super.initInnerData();
		declareProperty("AutoUse", ValueType.BOOL, false, false, false);
		declareProperty("LifeTime", ValueType.INT, false, false, false);
		declareProperty("EndTime", ValueType.LONG, false, false, false);
		declareProperty("Count", ValueType.INT, false, false, false);
	}

	public void onCreate() {
		super.onCreate();
	}

	public void onLoad() {
		super.onLoad();

		if (getInt("LifeTime") != -1 && getLong("EndTime") == 0l) {
			setProperty("EndTime", m_kernel.getServerTime() + getInt("LifeTime"));
		}
	}

	@Override
	public int getLeftTime() {
		if (getInt("LifeTime") == -1) {
			return -1;
		}
		int left = (int) (getLong("EndTime") - m_kernel.getServerTime());
		if (left < 0) {
			return 0;
		}
		return left;
	}
}
