package back.modules.data.mailmanage;

import back.modules.data.ItemData;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 发送邮件
 * Created by Administrator on 2018/5/10.
 */
public class SendMail implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //后台存储id
    private Integer[] receiver;  //收件人[如果为空或null则为指定渠道的所有玩家]
    private String title;   //邮件标题
    private int[] channelId;    //发送渠道[如果包含-1则为所有渠道]
    private String content; //邮件内容
    private ItemData[] attachment;    //附件
    private int expiry; //邮件有效期

    public SendMail() {
    }

    public SendMail(int id, Integer[] receiver, String title, int[] channelId, String content, ItemData[] attachment, int expiry) {

        this.id = id;
        this.receiver = receiver;
        this.title = title;
        this.channelId = channelId;
        this.content = content;
        this.attachment = attachment;
        this.expiry = expiry;
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer[] getReceiver() {
        return receiver;
    }

    public void setReceiver(Integer[] receiver) {
        this.receiver = receiver;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int[] getChannelId() {
        return channelId;
    }

    public void setChannelId(int[] channelId) {
        this.channelId = channelId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ItemData[] getAttachment() {
        return attachment;
    }

    public void setAttachment(ItemData[] attachment) {
        this.attachment = attachment;
    }

    public int getExpiry() {
        return expiry;
    }

    public void setExpiry(int expiry) {
        this.expiry = expiry;
    }

    @Override
    public String toString() {
        return "SendMail{" +
                "id=" + id +
                ", receiver=" + Arrays.toString(receiver) +
                ", title='" + title + '\'' +
                ", channelId=" + Arrays.toString(channelId) +
                ", content='" + content + '\'' +
                ", attachment=" + Arrays.toString(attachment) +
                ", expiry=" + expiry +
                '}';
    }
}
