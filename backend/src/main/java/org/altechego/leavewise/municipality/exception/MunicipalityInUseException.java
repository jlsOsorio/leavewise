package org.altechego.leavewise.municipality.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

// BR-15: still referenced by a location or holiday (ON DELETE RESTRICT)
public class MunicipalityInUseException extends ApiException {

    public MunicipalityInUseException(Long id) {
        super(ErrorCode.RESOURCE_IN_USE, "Municipality %d is used by a location or holiday".formatted(id));
    }
}
