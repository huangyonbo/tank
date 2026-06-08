package framework;

public class RecReqData {
    public int reqId;
    public int serId;
    public long time = 0;

    public boolean tick(long now){
        if (time == 0) {
            time = now;
            return false;
        }
        return now - time >= 3000;
    }
}
