package framework.master;

public interface IMasterModule {
	boolean onInit(MasterKernel kernel);
	void onDestroy();
}
