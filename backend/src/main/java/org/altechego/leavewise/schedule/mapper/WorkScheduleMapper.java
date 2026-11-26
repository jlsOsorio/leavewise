package org.altechego.leavewise.schedule.mapper;

import org.altechego.leavewise.schedule.dto.WorkScheduleDayResponse;
import org.altechego.leavewise.schedule.dto.WorkScheduleResponse;
import org.altechego.leavewise.schedule.entity.WorkSchedule;
import org.altechego.leavewise.schedule.entity.WorkScheduleDay;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkScheduleMapper {

    @Mapping(target = "companyId", source = "company.id")
    WorkScheduleResponse toResponse(WorkSchedule schedule);

    @Mapping(target = "isWorkingDay", source = "workingDay")
    WorkScheduleDayResponse toDayResponse(WorkScheduleDay day);
}
