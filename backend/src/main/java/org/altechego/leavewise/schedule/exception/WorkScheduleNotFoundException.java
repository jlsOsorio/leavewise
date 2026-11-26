package org.altechego.leavewise.schedule.exception;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;

public class WorkScheduleNotFoundException extends ApiException {

    public WorkScheduleNotFoundException(Long companyId) {
        super(ErrorCode.NOT_FOUND, "Company %d has no work schedule".formatted(companyId));
    }
}
