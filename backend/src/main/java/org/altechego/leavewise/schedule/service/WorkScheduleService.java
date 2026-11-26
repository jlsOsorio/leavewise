package org.altechego.leavewise.schedule.service;

import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.schedule.dto.WorkScheduleRequest;
import org.altechego.leavewise.schedule.dto.WorkScheduleResponse;

public interface WorkScheduleService {

    WorkScheduleResponse findByCompany(AuthenticatedUser actor, Long companyId);

    WorkScheduleResponse upsert(Long companyId, WorkScheduleRequest request);
}
