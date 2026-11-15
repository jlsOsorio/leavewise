package org.altechego.leavewise.company.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class DuplicateCompanyException extends ApiException {

    public DuplicateCompanyException() {
        super(ErrorCode.DUPLICATE, "A company with this name already exists");
    }
}
