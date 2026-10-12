package org.altechego.leavewise.common.config;

import org.altechego.leavewise.common.web.ApiMediaType;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] SUPPORTED_VERSIONS = {"1"};


    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useMediaTypeParameter(ApiMediaType.VND, ApiMediaType.VERSION_PARAM)
                .addSupportedVersions(SUPPORTED_VERSIONS)
                .setDefaultVersion(ApiMediaType.DEFAULT_VERSION);
    }
}
