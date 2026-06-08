package com.login.system.domain;

import lombok.Data;

@Data
public class UserRoleDO {
    private Long id;
    private Long userId;
    private Long roleId;

    @Override
    public String toString() {
        return "UserRoleDO{" +
                "id=" + id +
                ", userId=" + userId +
                ", roleId=" + roleId +
                '}';
    }
}
