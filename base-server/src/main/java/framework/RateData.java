package framework;

public class RateData {
	Object value;
	int rate;

	public RateData(Object value, int rate) {
		this.value = value;
		this.rate = rate;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public int getRate() {
		return rate;
	}

	public void setRate(int rate) {
		this.rate = rate;
	}
}
