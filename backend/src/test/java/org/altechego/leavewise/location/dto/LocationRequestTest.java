package org.altechego.leavewise.location.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LocationRequestTest {

    @Test
    void constructor_paddedValues_shouldTrimThem() {
        var request = new LocationRequest(" Sede  ", "  Rua A ", 7L);

        assertThat(request).extracting("name", "address").containsExactly("Sede", "Rua A");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void constructor_blankAddress_shouldBecomeNull(String address) {
        assertThat(new LocationRequest("Sede", address, 7L).address()).isNull();
    }

    @Test
    void constructor_nullName_shouldStayNullForValidation() {
        assertThat(new LocationRequest(null, null, 7L).name()).isNull();
    }
}
