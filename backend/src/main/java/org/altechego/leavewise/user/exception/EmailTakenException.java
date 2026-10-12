package org.altechego.leavewise.user.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class EmailTakenException extends ApiException {

    public EmailTakenException() {
        super(ErrorCode.EMAIL_TAKEN, "Email is already registered");
    }
}
