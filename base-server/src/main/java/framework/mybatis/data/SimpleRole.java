package framework.mybatis.data;

import lombok.Data;

import java.io.Serializable;

@Data
public class SimpleRole implements Serializable {
	private int id;
	private String userName;
	private int headId;
	private int proxyId;
}
