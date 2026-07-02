package com.example.user_service.outbox;

public final class UserOutboxEventTypes {

    public static final String USER_REGISTERED = "USER_REGISTERED";
    public static final String USER_LOGIN_SUCCESS = "USER_LOGIN_SUCCESS";
    public static final String USER_LOGIN_FAILED = "USER_LOGIN_FAILED";
    public static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String MFA_ENABLED = "MFA_ENABLED";

    private UserOutboxEventTypes() {
    }
}
