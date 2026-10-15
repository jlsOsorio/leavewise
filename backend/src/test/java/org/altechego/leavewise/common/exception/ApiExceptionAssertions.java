package org.altechego.leavewise.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

@UtilityClass
public class ApiExceptionAssertions {

    public static void assertErrorCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }
}
