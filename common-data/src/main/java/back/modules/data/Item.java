package back.modules.data;

import lombok.Data;

import java.io.Serializable;

@Data
public class Item implements Serializable {

    private static final long serialVersionUID = 4174090349169513676L;

    private String itemId;

    private Integer count;
}
