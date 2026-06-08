package back.modules.data.maintainset;


import java.io.Serializable;
import java.util.Arrays;

/**
 * 开启维护
 * Created by Administrator on 2018/4/11.
 */
public class StartMaintain implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;//维护记录id
    private int[] placeId;//渠道ID
    private String version;//客户端版本号
    private String message;//维护公告
    private long start; //维护开始时间
    private long end;//维护结束时间
    private int type;//维护类型 [详见MaintainType]

    public StartMaintain() {
    	
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getPlaceId() {
        return placeId;
    }

    public void setPlaceId(int[] placeId) {
        this.placeId = placeId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "StartMaintain{" +
                "id=" + id +
                ", placeId=" + Arrays.toString(placeId) +
                ", version='" + version + '\'' +
                ", message='" + message + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", type=" + type +
                '}';
    }
}
