package game.modules.items;

import lombok.Data;

import java.io.Serializable;

/**
 * 对应 PropertyItem.xml 里的属性
 */
@Data
public class XmlPropertyItem implements Serializable {

    private static final long serialVersionUID = 5539775917687196677L;
    /**
     * ID
     */
    private String Id;

    /**
     * 名称
     */
    private String name;

    /**
     * 对应属性名
     */
    private String property;

}
