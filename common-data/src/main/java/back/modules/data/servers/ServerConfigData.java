package back.modules.data.servers;


public class ServerConfigData {
	int id;
	String logicName;
	String name;
	String type;
	String addr;
	int port;
	boolean front = false;
	String frontAddr = "";
	int frontPort = 0;
	String next;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getLogicName() {
		return logicName;
	}
	public void setLogicName(String logicName) {
		this.logicName = logicName;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAddr() {
		return addr;
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public boolean isFront() {
		return front;
	}
	public void setFront(boolean front) {
		this.front = front;
	}
	public String getFrontAddr() {
		return frontAddr;
	}
	public void setFrontAddr(String frontAddr) {
		this.frontAddr = frontAddr;
	}
	public int getFrontPort() {
		return frontPort;
	}
	public void setFrontPort(int frontPort) {
		this.frontPort = frontPort;
	}
	public String getNext() {
		return next;
	}
	public void setNext(String next) {
		this.next = next;
	}
}
