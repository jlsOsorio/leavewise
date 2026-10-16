package org.altechego.leavewise.common.web;

import java.util.Set;

import lombok.experimental.UtilityClass;
import org.altechego.leavewise.common.exception.UnsupportedSortPropertyException;
import org.springframework.data.domain.Pageable;

@UtilityClass
public class SortRestriction {

    public static void requireAllowed(Pageable pageable, Set<String> allowedProperties) {
        pageable.getSort().stream()
                .filter(order -> !allowedProperties.contains(order.getProperty()))
                .findFirst()
                .ifPresent(order -> {
                    throw new UnsupportedSortPropertyException(order.getProperty(), allowedProperties);
                });
    }
}
