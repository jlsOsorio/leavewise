package org.altechego.leavewise.schedule.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WorkScheduleConstants {

    public static final int NAME_MAX_LENGTH = 150;
    public static final int FIRST_DAY_OF_WEEK = 1;
    public static final int DAYS_PER_WEEK = 7;

    public static final String PATH = "/api/companies/{companyId}/work-schedule";
}
