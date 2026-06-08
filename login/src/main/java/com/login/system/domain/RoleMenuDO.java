package com.login.system.domain;

import lombok.Data;

@Data
public class RoleMenuDO {
	private Long id;
	private Long  roleId;
	private Long menuId;
	@Override
	public String toString() {
		return "RoleMenuDO{" +
				"id=" + id +
				", roleId=" + roleId +
				", menuId=" + menuId +
				'}';
	}
}
