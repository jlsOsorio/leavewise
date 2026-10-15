package org.altechego.leavewise.auth.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid or expired refresh token");
    }
}
