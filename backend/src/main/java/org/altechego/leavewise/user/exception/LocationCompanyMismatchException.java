package org.altechego.leavewise.user.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class LocationCompanyMismatchException extends ApiException {

    public LocationCompanyMismatchException() {
        super(ErrorCode.LOCATION_COMPANY_MISMATCH, "Location does not belong to the company");
    }
}
