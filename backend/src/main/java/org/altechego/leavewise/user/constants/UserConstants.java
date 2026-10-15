package org.altechego.leavewise.user.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserConstants {

    public static final int NAME_MAX_LENGTH = 200;
    public static final int NIF_LENGTH = 9;
    public static final int EMAIL_MAX_LENGTH = 320;
    public static final int ROLE_MAX_LENGTH = 30;
    public static final int CONTACT_MAX_LENGTH = 50;
    public static final String EMAIL_UNIQUE_CONSTRAINT = "uk_lw_user_email";
    public static final String NIF_UNIQUE_CONSTRAINT = "uk_lw_user_nif";
}
