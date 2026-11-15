package org.altechego.leavewise.location.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class LocationInUseException extends ApiException {

    public LocationInUseException(Long id) {
        super(ErrorCode.RESOURCE_IN_USE, "Location %d still has users".formatted(id));
    }
}
