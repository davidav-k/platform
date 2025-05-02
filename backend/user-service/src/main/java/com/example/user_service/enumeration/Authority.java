package com.example.user_service.enumeration;

import static com.example.user_service.constant.Constants.*;

public enum Authority {

    USER(USER_AUTHORITIES),

    MANAGER(MANAGER_AUTHORITIES),

    ADMIN(ADMIN_AUTHORITIES),

    SUPER_ADMIN(SUPER_ADMIN_AUTHORITIES);

    private final String value;

    Authority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
