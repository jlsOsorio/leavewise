package org.altechego.leavewise.common.web;

import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

@UtilityClass
public class ApiMediaType {

    public static final String JSON = "application/vnd.leavewise+json";
    public static final MediaType VND = MediaType.parseMediaType(JSON);
    public static final String VERSION_PARAM = "version";
    public static final String DEFAULT_VERSION = "1";
}
