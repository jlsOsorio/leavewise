package org.altechego.leavewise.common.exception;

import java.util.Collection;

public class UnsupportedSortPropertyException extends ApiException {

    public UnsupportedSortPropertyException(String property, Collection<String> allowedProperties) {
        super(ErrorCode.VALIDATION_ERROR, "Unsupported sort property '%s'; allowed: %s"
                .formatted(property, String.join(", ", allowedProperties.stream().sorted().toList())));
    }
}
