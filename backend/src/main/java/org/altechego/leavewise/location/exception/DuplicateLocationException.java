package org.altechego.leavewise.location.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class DuplicateLocationException extends ApiException {

    public DuplicateLocationException() {
        super(ErrorCode.DUPLICATE, "The company already has a location with this name");
    }
}
