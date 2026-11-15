package org.altechego.leavewise.company.constants;

import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CompanyConstants {

    public static final int NAME_MAX_LENGTH = 200;

    public static final String UNIQUE_CONSTRAINT = "uk_company_name";

    public static final String BASE_PATH = "/api/companies";
    public static final String DEFAULT_SORT = "name";
    public static final Set<String> SORTABLE_FIELDS = Set.of(DEFAULT_SORT);
}
