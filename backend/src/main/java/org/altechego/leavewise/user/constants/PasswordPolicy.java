package org.altechego.leavewise.user.constants;

import java.nio.charset.StandardCharsets;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_UTF8_BYTES = 72;


    public static boolean fitsBcryptLimit(String password) {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }
}
