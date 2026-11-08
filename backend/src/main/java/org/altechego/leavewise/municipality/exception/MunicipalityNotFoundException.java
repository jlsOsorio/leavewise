package org.altechego.leavewise.municipality.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class MunicipalityNotFoundException extends ApiException {

    public MunicipalityNotFoundException(Long id) {
        super(ErrorCode.NOT_FOUND, "Municipality %d not found".formatted(id));
    }
}
