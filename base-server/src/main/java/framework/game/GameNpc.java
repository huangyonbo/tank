package framework.game;

class GameNpc extends GameObject {
	public GameNpc(Kernel kernel) {
		super(kernel);
		m_Type = GameObjectType.GOTYPE_NPC;
	}

	public void onCreate() {
		super.onCreate();
		m_kernel.addNpc(this);
	}

	public void onDestroy() {
		super.onDestroy();
		m_kernel.removeNpc(this);
	}
}
