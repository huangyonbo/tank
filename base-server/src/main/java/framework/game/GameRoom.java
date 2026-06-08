package framework.game;

/**
 * 
 * 描述： 房间对象
 * 
 */
class GameRoom extends GameObject {
	public GameRoom(Kernel kernel) {
		super(kernel);
		m_Type = GameObjectType.GOTYPE_ROOM;
	}
}