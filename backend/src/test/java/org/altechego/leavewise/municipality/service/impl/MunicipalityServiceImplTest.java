package org.altechego.leavewise.municipality.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.mapper.MunicipalityMapper;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MunicipalityServiceImplTest {

    @Mock
    private MunicipalityRepository municipalityRepository;

    private MunicipalityServiceImpl municipalityService;


    @BeforeEach
    void setUp() {
        municipalityService = new MunicipalityServiceImpl(
                municipalityRepository, Mappers.getMapper(MunicipalityMapper.class));
    }

    @Test
    void findOrCreate_municipalityExists_shouldReturnItWithoutSaving() {
        var existing = Municipality.builder().id(7L).name("Lisboa").district("Lisboa").country("Portugal").build();
        when(municipalityRepository.findByNameIgnoreCaseAndDistrictIgnoreCaseAndCountryIgnoreCase(
                "Lisboa", "Lisboa", "Portugal")).thenReturn(Optional.of(existing));

        var response = municipalityService.findOrCreate("Lisboa", "Lisboa", "Portugal");

        assertThat(response.id()).isEqualTo(7L);
        verify(municipalityRepository, never()).save(any());
    }

    @Test
    void findOrCreate_municipalityMissing_shouldSaveAndReturnIt() {
        when(municipalityRepository.findByNameIgnoreCaseAndDistrictIgnoreCaseAndCountryIgnoreCase(
                "Porto", "Porto", "Portugal")).thenReturn(Optional.empty());
        when(municipalityRepository.save(any(Municipality.class))).thenAnswer(call -> call.getArgument(0));

        var response = municipalityService.findOrCreate("Porto", "Porto", "Portugal");

        var saved = ArgumentCaptor.forClass(Municipality.class);
        verify(municipalityRepository).save(saved.capture());
        assertThat(saved.getValue())
                .extracting(Municipality::getName, Municipality::getDistrict, Municipality::getCountry)
                .containsExactly("Porto", "Porto", "Portugal");
        assertThat(response.name()).isEqualTo("Porto");
    }
}
