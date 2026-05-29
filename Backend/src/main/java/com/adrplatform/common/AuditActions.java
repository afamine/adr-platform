package com.adrplatform.common;

public final class AuditActions {

    private AuditActions() {}

    // --- ADR ---
    public static final String ADR_CREATED        = "ADR_CREATED";
    public static final String ADR_UPDATED        = "ADR_UPDATED";
    public static final String ADR_DELETED        = "ADR_DELETED";
    public static final String ADR_STATUS_CHANGED = "ADR_STATUS_CHANGED";

    // --- Vote ---
    public static final String VOTE_CAST          = "VOTE_CAST";

    // --- Comment ---
    public static final String COMMENT_ADDED      = "COMMENT_ADDED";

    // --- User ---
    public static final String USER_REGISTERED      = "USER_REGISTERED";
    public static final String USER_LOGGED_IN       = "USER_LOGGED_IN";
    public static final String USER_LOGGED_OUT      = "USER_LOGGED_OUT";
    public static final String USER_EMAIL_VERIFIED  = "USER_EMAIL_VERIFIED";
    public static final String USER_INVITE_ACCEPTED = "USER_INVITE_ACCEPTED";
    public static final String USER_INVITED         = "USER_INVITED";
    public static final String USER_STATUS_CHANGED  = "USER_STATUS_CHANGED";
    public static final String PROFILE_UPDATED      = "PROFILE_UPDATED";
    public static final String ROLE_CHANGED         = "ROLE_CHANGED";
    public static final String PASSWORD_CHANGED     = "PASSWORD_CHANGED";
    public static final String TOKEN_REFRESHED      = "TOKEN_REFRESHED";
    public static final String LOGOUT_ALL_DEVICES   = "LOGOUT_ALL_DEVICES";

    // --- Workspace ---
    public static final String WORKSPACE_UPDATED  = "WORKSPACE_UPDATED";
    public static final String WORKSPACE_RESET    = "WORKSPACE_RESET";
}
