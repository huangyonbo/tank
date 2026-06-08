package framework.pub;

public interface IPubSpace {
	IPubData createPubData(String name);

	IPubData getPubData(String name);

	void deletePubData(String name);
}
