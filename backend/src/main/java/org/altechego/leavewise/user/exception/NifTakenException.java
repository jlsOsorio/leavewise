package org.altechego.leavewise.user.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class NifTakenException extends ApiException {

    public NifTakenException() {
        super(ErrorCode.NIF_TAKEN, "NIF is already registered");
    }
}
