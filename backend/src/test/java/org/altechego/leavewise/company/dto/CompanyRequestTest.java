package org.altechego.leavewise.company.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompanyRequestTest {

    @Test
    void constructor_paddedName_shouldTrimIt() {
        assertThat(new CompanyRequest("  Acme ").name()).isEqualTo("Acme");
    }

    @Test
    void constructor_nullName_shouldStayNullForValidation() {
        assertThat(new CompanyRequest(null).name()).isNull();
    }
}
