package back.modules.data.feedback;

import back.modules.data.ItemData;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 回复玩家反馈
 * Created by Administrator on 2018/5/4.
 */
public class Reply implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //反馈id
    private int uid;    //玩家id
    private String replyText;   //回复文本
    private List<ItemData> attachment;  //附件

    public Reply() {
    }

    public Reply(int id, int uid, String replyText, List<ItemData> attachment) {

        this.id = id;
        this.uid = uid;
        this.replyText = replyText;
        this.attachment = attachment;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public List<ItemData> getAttachment() {
        return attachment;
    }

    public void setAttachment(List<ItemData> attachment) {
        this.attachment = attachment;
    }

    @Override

    public String toString() {
        return "Reply{" +
                "id=" + id +
                "uid=" + uid +
                ", replyText='" + replyText + '\'' +
                ", attachment=" + Arrays.deepToString(attachment.toArray()) +
                '}';
    }
}
