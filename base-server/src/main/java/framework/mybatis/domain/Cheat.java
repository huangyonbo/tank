package framework.mybatis.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@RequiredArgsConstructor
public class Cheat implements Serializable {
    @TableId(type = IdType.AUTO)
    protected Integer id;
    @NonNull
    private Integer uid;
    @NonNull
    private Long intervalTime;
    @NonNull
    private String ip;
    @NonNull
    private Date time;
}
