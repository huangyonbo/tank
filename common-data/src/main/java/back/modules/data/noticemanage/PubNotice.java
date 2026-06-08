package back.modules.data.noticemanage;


import java.io.Serializable;
import java.util.Arrays;

/**
 * 发布公告
 * Created by Administrator on 2018/4/18.
 */
public class PubNotice implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id; //公告记录id
    private int type;   //公告类型[详见NoticeType]
    private String title;   //公告标题
    private String tag; //公告标签
    private int[] placeId;  //公告渠道[详见配置]
    private String content; //公告内容
    private String picture; //公告图片[路径，且不含域名或端口]
    private boolean mail;   //是否发送邮件[游戏公告时有效, true-发送 false-不发送]

    @Override
    public String toString() {
        return "PubNotice{" +
                "id=" + id +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", tag='" + tag + '\'' +
                ", placeId=" + Arrays.toString(placeId) +
                ", content='" + content + '\'' +
                ", picture='" + picture + '\'' +
                ", mail=" + mail +
                '}';
    }

    public PubNotice() {
    }

    public PubNotice(int id, int type, String title, String tag, int[] placeId, String content, String picture, boolean mail) {
        this.id = id;
        this.type = type;
        this.title = (title == null ? "" : title);
        this.tag = (tag == null ? "" : tag);
        this.placeId = placeId;
        this.content = (content == null ? "" : content);
        this.picture = (picture == null ? "" : picture);
        this.mail = mail;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int[] getPlaceId() {
        return placeId;
    }

    public void setPlaceId(int[] placeId) {
        this.placeId = placeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public boolean isMail() {
        return mail;
    }

    public void setMail(boolean mail) {
        this.mail = mail;
    }
}
