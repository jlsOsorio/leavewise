package org.altechego.leavewise.auth.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class AccountDisabledException extends ApiException {

    public AccountDisabledException() {
        super(ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
    }
}
