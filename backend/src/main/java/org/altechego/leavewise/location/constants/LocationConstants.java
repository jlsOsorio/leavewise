package org.altechego.leavewise.location.constants;

import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LocationConstants {

    public static final int NAME_MAX_LENGTH = 150;
    public static final int ADDRESS_MAX_LENGTH = 500;

    public static final String UNIQUE_CONSTRAINT = "uk_company_location_company_name";

    public static final String BASE_PATH = "/api/locations";
    public static final String COMPANY_LOCATIONS_PATH = "/api/companies/{companyId}/locations";
    public static final String DEFAULT_SORT = "name";
    public static final Set<String> SORTABLE_FIELDS = Set.of(DEFAULT_SORT);
}
