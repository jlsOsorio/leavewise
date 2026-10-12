package org.altechego.leavewise.company.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class CompanyNotFoundException extends ApiException {

    public CompanyNotFoundException(Long id) {
        super(ErrorCode.NOT_FOUND, "Company %d not found".formatted(id));
    }
}
