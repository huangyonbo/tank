package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class PlayerWeaponState {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer uid;
    private String weaponsLevels;
    private Integer tankSpeedLevel;
    private Integer tankArmorLevel;
}
