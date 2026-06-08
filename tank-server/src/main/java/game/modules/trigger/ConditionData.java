package game.modules.trigger;

import lombok.Data;

import java.util.List;

@Data
public class ConditionData {

    private Integer id;

    private Integer triggerId;

    private int[] roomLimit;

    private Integer bvLimit;

    private List<String> targets;
}
