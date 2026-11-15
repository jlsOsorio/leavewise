package org.altechego.leavewise.location.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class LocationNotFoundException extends ApiException {

    public LocationNotFoundException(Long id) {
        super(ErrorCode.NOT_FOUND, "Location %d not found".formatted(id));
    }
}
