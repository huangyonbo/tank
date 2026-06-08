package framework.game;

public class IntList {
	// static {
	// String config = System.getProperty("user.dir");
	// System.load(config + File.separator + "PodList.dll");
	// }

	public native void add(int e);

	public native void add(int index, int e);

	public native int get(int index);

	public native int remove(int index);

	public native void clear();

	public native int size();
}
