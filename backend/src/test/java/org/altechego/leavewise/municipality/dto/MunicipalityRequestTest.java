package org.altechego.leavewise.municipality.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.altechego.leavewise.municipality.constants.MunicipalityConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MunicipalityRequestTest {

    @Test
    void constructor_paddedValues_shouldTrimThem() {
        var request = new MunicipalityRequest("  Braga ", " Braga  ", " Spain ");

        assertThat(request).extracting("name", "district", "country").containsExactly("Braga", "Braga", "Spain");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void constructor_blankCountry_shouldDefaultToPortugal(String country) {
        var request = new MunicipalityRequest("Braga", "Braga", country);

        assertThat(request.country()).isEqualTo(MunicipalityConstants.DEFAULT_COUNTRY);
    }

    @Test
    void constructor_nullName_shouldStayNullForValidation() {
        var request = new MunicipalityRequest(null, "Braga", null);

        assertThat(request.name()).isNull();
    }
}
