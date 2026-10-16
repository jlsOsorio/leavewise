package org.altechego.leavewise.auth.constants;

import java.time.Duration;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuthConstants {

    public static final int TOKEN_HASH_LENGTH = 64;
    public static final int REFRESH_TOKEN_BYTES = 32;
    // Concurrent refreshes from the BFF proxy may present a token that was just rotated
    public static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    public static final String LOOKUP_DEFAULT_SORT = "name";
    public static final Set<String> LOOKUP_SORTABLE_FIELDS = Set.of(LOOKUP_DEFAULT_SORT);
}
