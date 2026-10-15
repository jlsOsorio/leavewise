package org.altechego.leavewise.common.exception;

public class UnsupportedSortPropertyException extends ApiException {

    public UnsupportedSortPropertyException(String property) {
        super(ErrorCode.VALIDATION_ERROR, "Unsupported sort property '%s'".formatted(property));
    }
}
