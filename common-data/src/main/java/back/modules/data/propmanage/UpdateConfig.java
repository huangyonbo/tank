package back.modules.data.propmanage;


import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Administrator on 2018/5/7.
 */
public class UpdateConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String[] filePath;    //配置文件路径[不含域名或端口，且以"/"开头]

    public UpdateConfig() {
    }

    public UpdateConfig(String[] filePath) {
        this.filePath = filePath;
    }

    public String[] getFilePath() {
        return filePath;
    }

    public void setFilePath(String[] filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "UpdateConfig{" +
                "filePath=" + Arrays.toString(filePath) +
                '}';
    }
}
