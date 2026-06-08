package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class CustomMapRecord {
    @TableId(type = IdType.AUTO)
    private Long mapId;
    private Integer ownerUid;
    private String mapName;
    private String mapData;
    private Integer width;
    private Integer height;
    private Long heat;
    /** 地图难度（未完成次数累计），用于最难榜排序 */
    private Integer difficulty;
    /** 游玩次数（成功调用开始接口且落库 +1） */
    private Long playCount;
    /** 游玩成功次数 */
    private Long successCount;
    /** 游玩失败次数（结束且未完成） */
    private Long failCount;
    /** 点赞总数（有效点赞落库累计） */
    private Long likeCount;
    private Date createTime;
    private Date updateTime;
}
