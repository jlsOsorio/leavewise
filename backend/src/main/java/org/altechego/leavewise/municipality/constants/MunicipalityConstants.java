package org.altechego.leavewise.municipality.constants;

import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MunicipalityConstants {

    public static final int NAME_MAX_LENGTH = 150;
    public static final int DISTRICT_MAX_LENGTH = 150;
    public static final int COUNTRY_MAX_LENGTH = 100;

    public static final String DEFAULT_COUNTRY = "Portugal";

    public static final String UNIQUE_CONSTRAINT = "uk_municipality_name_district_country";

    public static final String BASE_PATH = "/api/municipalities";
    public static final String DEFAULT_SORT = "name";
    public static final Set<String> SORTABLE_FIELDS = Set.of(DEFAULT_SORT, "district", "country");
}
