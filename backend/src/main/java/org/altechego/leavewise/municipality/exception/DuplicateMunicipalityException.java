package org.altechego.leavewise.municipality.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class DuplicateMunicipalityException extends ApiException {

    public DuplicateMunicipalityException() {
        super(ErrorCode.DUPLICATE, "A municipality with this name, district and country already exists");
    }
}
