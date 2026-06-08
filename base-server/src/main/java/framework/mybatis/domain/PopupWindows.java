package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class PopupWindows {
    @TableId(type = IdType.AUTO)
    private Integer id = 0;
    private Integer popupId = 0;
    private Integer typeId = 0;
    private Integer times = 0;
    private Integer triggerTime = 0;
    private Integer level = 0;
}
