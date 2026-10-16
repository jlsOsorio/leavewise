package org.altechego.leavewise.user.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(Long id) {
        super(ErrorCode.NOT_FOUND, "User %d not found".formatted(id));
    }
}
