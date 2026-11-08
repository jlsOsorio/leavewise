package org.altechego.leavewise.municipality.service.impl;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;
import org.altechego.leavewise.municipality.dto.MunicipalityRequest;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.mapper.MunicipalityMapper;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MunicipalityServiceImplTest {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String RESTRICT_VIOLATION = "23001";
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private MunicipalityRepository municipalityRepository;

    @Mock
    private CompanyLocationRepository locationRepository;

    private MunicipalityServiceImpl municipalityService;


    @BeforeEach
    void setUp() {
        municipalityService = new MunicipalityServiceImpl(
                municipalityRepository, Mappers.getMapper(MunicipalityMapper.class), locationRepository);
    }

    @Test
    void findOrCreate_municipalityExists_shouldReturnItWithoutSaving() {
        var existing = municipality(7L, "Lisboa");
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

    @Test
    void search_blankQuery_shouldListAll() {
        when(municipalityRepository.findAll(FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(municipality(1L, "Braga")), FIRST_PAGE, 1));

        var page = municipalityService.search("  ", FIRST_PAGE);

        assertThat(page.content()).extracting("name").containsExactly("Braga");
    }

    @Test
    void search_query_shouldFilterByTrimmedName() {
        when(municipalityRepository.findByNameContainingIgnoreCase("bra", FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(municipality(1L, "Braga")), FIRST_PAGE, 1));

        var page = municipalityService.search(" bra ", FIRST_PAGE);

        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findById_unknownId_shouldThrowNotFound() {
        when(municipalityRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> municipalityService.findById(99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void create_validRequest_shouldSaveMappedMunicipality() {
        when(municipalityRepository.saveAndFlush(any(Municipality.class))).thenAnswer(call -> call.getArgument(0));

        var response = municipalityService.create(new MunicipalityRequest("Braga", "Braga", null));

        assertThat(response)
                .extracting("name", "district", "country")
                .containsExactly("Braga", "Braga", MunicipalityConstants.DEFAULT_COUNTRY);
    }

    @Test
    void create_uniqueIndexViolated_shouldThrowDuplicate() {
        when(municipalityRepository.saveAndFlush(any(Municipality.class)))
                .thenThrow(integrityViolation(UNIQUE_VIOLATION, MunicipalityConstants.UNIQUE_CONSTRAINT));

        assertErrorCode(
                () -> municipalityService.create(new MunicipalityRequest("Braga", "Braga", null)),
                ErrorCode.DUPLICATE);
    }

    @Test
    void create_otherIntegrityViolation_shouldRethrow() {
        var violation = integrityViolation(UNIQUE_VIOLATION, "some_other_constraint");
        when(municipalityRepository.saveAndFlush(any(Municipality.class))).thenThrow(violation);

        assertThatThrownBy(() -> municipalityService.create(new MunicipalityRequest("Braga", "Braga", null)))
                .isSameAs(violation);
    }

    @Test
    void update_existingMunicipality_shouldApplyRequest() {
        var existing = municipality(5L, "Braga");
        when(municipalityRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(municipalityRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = municipalityService.update(5L, new MunicipalityRequest("Guimarães", "Braga", "Portugal"));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Guimarães");
    }

    @Test
    void update_unknownId_shouldThrowNotFound() {
        when(municipalityRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> municipalityService.update(99L, new MunicipalityRequest("Braga", "Braga", null)),
                ErrorCode.NOT_FOUND);
    }

    @Test
    void update_uniqueIndexViolated_shouldThrowDuplicate() {
        var existing = municipality(5L, "Braga");
        when(municipalityRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(municipalityRepository.saveAndFlush(existing))
                .thenThrow(integrityViolation(UNIQUE_VIOLATION, MunicipalityConstants.UNIQUE_CONSTRAINT));

        assertErrorCode(
                () -> municipalityService.update(5L, new MunicipalityRequest("Porto", "Porto", null)),
                ErrorCode.DUPLICATE);
    }

    @Test
    void delete_unreferencedMunicipality_shouldDeleteIt() {
        var existing = municipality(5L, "Braga");
        when(municipalityRepository.findById(5L)).thenReturn(Optional.of(existing));

        municipalityService.delete(5L);

        verify(municipalityRepository).delete(existing);
        verify(municipalityRepository).flush();
    }

    @Test
    void delete_unknownId_shouldThrowNotFound() {
        when(municipalityRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> municipalityService.delete(99L), ErrorCode.NOT_FOUND);
    }

    // BR-15
    @Test
    void delete_referencedByLocation_shouldThrowResourceInUseWithoutDeleting() {
        when(municipalityRepository.findById(5L)).thenReturn(Optional.of(municipality(5L, "Braga")));
        when(locationRepository.existsByMunicipalityId(5L)).thenReturn(true);

        assertErrorCode(() -> municipalityService.delete(5L), ErrorCode.RESOURCE_IN_USE);
        verify(municipalityRepository, never()).delete(any());
    }

    @Test
    void delete_referencedAfterCheck_shouldThrowResourceInUse() {
        when(municipalityRepository.findById(5L)).thenReturn(Optional.of(municipality(5L, "Braga")));
        doThrow(integrityViolation(RESTRICT_VIOLATION, "fk_company_location_municipality"))
                .when(municipalityRepository).flush();

        assertErrorCode(() -> municipalityService.delete(5L), ErrorCode.RESOURCE_IN_USE);
    }


    private static Municipality municipality(Long id, String name) {
        return Municipality.builder().id(id).name(name).district(name).country("Portugal").build();
    }

    private static DataIntegrityViolationException integrityViolation(String sqlState, String constraint) {
        var cause = new ConstraintViolationException("violation", new SQLException("violation", sqlState), constraint);
        return new DataIntegrityViolationException("violation", cause);
    }
}
