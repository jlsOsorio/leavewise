package org.altechego.leavewise.company.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class CompanyInUseException extends ApiException {

    public CompanyInUseException(Long id) {
        super(ErrorCode.RESOURCE_IN_USE, "Company %d still has users".formatted(id));
    }
}
