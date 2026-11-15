package org.altechego.leavewise.location.service.impl;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.constants.LocationConstants;
import org.altechego.leavewise.location.dto.LocationRequest;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.location.mapper.LocationMapper;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.location.service.LocationReferenceChecker;
import org.altechego.leavewise.municipality.entity.Municipality;
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
class LocationServiceImplTest {

    private static final long COMPANY_ID = 3L;
    private static final long MUNICIPALITY_ID = 7L;
    private static final long OTHER_MUNICIPALITY_ID = 8L;
    private static final long LOCATION_ID = 11L;
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String RESTRICT_VIOLATION = "23001";
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Mock
    private CompanyLocationRepository locationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    @Mock
    private LocationReferenceChecker referenceChecker;

    private LocationServiceImpl locationService;


    @BeforeEach
    void setUp() {
        locationService = new LocationServiceImpl(
                locationRepository,
                companyRepository,
                municipalityRepository,
                Mappers.getMapper(LocationMapper.class),
                List.of(referenceChecker));
    }

    @Test
    void findOrCreate_locationExists_shouldReturnItWithoutSaving() {
        when(locationRepository.findByCompanyIdAndName(COMPANY_ID, "Sede")).thenReturn(Optional.of(location()));

        var response = locationService.findOrCreate(COMPANY_ID, MUNICIPALITY_ID, "Sede");

        assertThat(response.id()).isEqualTo(LOCATION_ID);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void findOrCreate_locationMissing_shouldSaveItInTheCompanyAndMunicipality() {
        var company = company();
        var municipality = municipality(MUNICIPALITY_ID);
        when(locationRepository.findByCompanyIdAndName(COMPANY_ID, "Sede")).thenReturn(Optional.empty());
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(municipalityRepository.getReferenceById(MUNICIPALITY_ID)).thenReturn(municipality);
        when(locationRepository.save(any(CompanyLocation.class))).thenAnswer(call -> call.getArgument(0));

        var response = locationService.findOrCreate(COMPANY_ID, MUNICIPALITY_ID, "Sede");

        var saved = ArgumentCaptor.forClass(CompanyLocation.class);
        verify(locationRepository).save(saved.capture());
        assertThat(saved.getValue().getCompany()).isSameAs(company);
        assertThat(saved.getValue().getMunicipality()).isSameAs(municipality);
        assertThat(response.companyId()).isEqualTo(COMPANY_ID);
        assertThat(response.municipality().id()).isEqualTo(MUNICIPALITY_ID);
        assertThat(response.name()).isEqualTo("Sede");
    }

    @Test
    void findByCompany_existingCompany_shouldReturnPageOfLocationSummaries() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(locationRepository.findByCompanyId(COMPANY_ID, FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(location()), FIRST_PAGE, 1));

        var page = locationService.findByCompany(COMPANY_ID, FIRST_PAGE);

        assertThat(page.content()).containsExactly(new LocationSummaryResponse(LOCATION_ID, "Sede"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findByCompany_unknownCompany_shouldThrowNotFound() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> locationService.findByCompany(COMPANY_ID, FIRST_PAGE), ErrorCode.NOT_FOUND);
        verify(locationRepository, never()).findByCompanyId(any(), any());
    }

    @Test
    void findDetailsByCompany_existingCompany_shouldIncludeMunicipality() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(locationRepository.findWithMunicipalityByCompanyId(COMPANY_ID, FIRST_PAGE))
                .thenReturn(new PageImpl<>(List.of(location()), FIRST_PAGE, 1));

        var page = locationService.findDetailsByCompany(COMPANY_ID, FIRST_PAGE);

        assertThat(page.content().getFirst().municipality().name()).isEqualTo("Braga");
    }

    @Test
    void findDetailsByCompany_unknownCompany_shouldThrowNotFound() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> locationService.findDetailsByCompany(COMPANY_ID, FIRST_PAGE), ErrorCode.NOT_FOUND);
    }

    @Test
    void findById_unknownId_shouldThrowNotFound() {
        when(locationRepository.findWithMunicipalityById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> locationService.findById(99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void create_validRequest_shouldSaveItInTheCompanyAndMunicipality() {
        var company = company();
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(municipalityRepository.findById(MUNICIPALITY_ID)).thenReturn(Optional.of(municipality(MUNICIPALITY_ID)));
        when(locationRepository.saveAndFlush(any(CompanyLocation.class))).thenAnswer(call -> call.getArgument(0));

        var response = locationService.create(COMPANY_ID, request(MUNICIPALITY_ID));

        var saved = ArgumentCaptor.forClass(CompanyLocation.class);
        verify(locationRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCompany()).isSameAs(company);
        assertThat(response).extracting("companyId", "name", "address").containsExactly(COMPANY_ID, "Sede", "Rua A");
        assertThat(response.municipality().id()).isEqualTo(MUNICIPALITY_ID);
    }

    @Test
    void create_unknownCompany_shouldThrowNotFoundWithoutSaving() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> locationService.create(COMPANY_ID, request(MUNICIPALITY_ID)), ErrorCode.NOT_FOUND);
        verify(locationRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_unknownMunicipality_shouldThrowNotFoundWithoutSaving() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company());
        when(municipalityRepository.findById(MUNICIPALITY_ID)).thenReturn(Optional.empty());

        assertErrorCode(() -> locationService.create(COMPANY_ID, request(MUNICIPALITY_ID)), ErrorCode.NOT_FOUND);
        verify(locationRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_nameTakenInCompany_shouldThrowDuplicate() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company());
        when(municipalityRepository.findById(MUNICIPALITY_ID)).thenReturn(Optional.of(municipality(MUNICIPALITY_ID)));
        when(locationRepository.saveAndFlush(any(CompanyLocation.class)))
                .thenThrow(integrityViolation(UNIQUE_VIOLATION, LocationConstants.UNIQUE_CONSTRAINT));

        assertErrorCode(() -> locationService.create(COMPANY_ID, request(MUNICIPALITY_ID)), ErrorCode.DUPLICATE);
    }

    @Test
    void update_sameMunicipality_shouldApplyRequestWithoutLoadingMunicipality() {
        var existing = location();
        when(locationRepository.findWithMunicipalityById(LOCATION_ID)).thenReturn(Optional.of(existing));
        when(locationRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = locationService.update(LOCATION_ID, request(MUNICIPALITY_ID));

        assertThat(response).extracting("name", "address").containsExactly("Sede", "Rua A");
        verify(municipalityRepository, never()).findById(any());
    }

    @Test
    void update_otherMunicipality_shouldMoveLocationToIt() {
        var existing = location();
        when(locationRepository.findWithMunicipalityById(LOCATION_ID)).thenReturn(Optional.of(existing));
        when(municipalityRepository.findById(OTHER_MUNICIPALITY_ID))
                .thenReturn(Optional.of(municipality(OTHER_MUNICIPALITY_ID)));
        when(locationRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = locationService.update(LOCATION_ID, request(OTHER_MUNICIPALITY_ID));

        assertThat(response.municipality().id()).isEqualTo(OTHER_MUNICIPALITY_ID);
    }

    @Test
    void update_unknownMunicipality_shouldThrowNotFound() {
        when(locationRepository.findWithMunicipalityById(LOCATION_ID)).thenReturn(Optional.of(location()));
        when(municipalityRepository.findById(OTHER_MUNICIPALITY_ID)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> locationService.update(LOCATION_ID, request(OTHER_MUNICIPALITY_ID)), ErrorCode.NOT_FOUND);
    }

    @Test
    void update_unknownId_shouldThrowNotFound() {
        when(locationRepository.findWithMunicipalityById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> locationService.update(99L, request(MUNICIPALITY_ID)), ErrorCode.NOT_FOUND);
    }

    @Test
    void delete_unreferencedLocation_shouldDeleteIt() {
        var existing = location();
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(existing));

        locationService.delete(LOCATION_ID);

        verify(locationRepository).delete(existing);
        verify(locationRepository).flush();
    }

    @Test
    void delete_unknownId_shouldThrowNotFound() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> locationService.delete(99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void delete_referenced_shouldThrowResourceInUseWithoutDeleting() {
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location()));
        when(referenceChecker.isLocationInUse(LOCATION_ID)).thenReturn(true);

        assertErrorCode(() -> locationService.delete(LOCATION_ID), ErrorCode.RESOURCE_IN_USE);
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void delete_referencedAfterCheck_shouldThrowResourceInUse() {
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location()));
        doThrow(integrityViolation(RESTRICT_VIOLATION, "fk_lw_user_company_location"))
                .when(locationRepository).flush();

        assertErrorCode(() -> locationService.delete(LOCATION_ID), ErrorCode.RESOURCE_IN_USE);
    }


    private static Company company() {
        return Company.builder().id(COMPANY_ID).name("LeaveWise").build();
    }

    private static Municipality municipality(Long id) {
        return Municipality.builder().id(id).name("Braga").district("Braga").country("Portugal").build();
    }

    private static CompanyLocation location() {
        return CompanyLocation.builder()
                .id(LOCATION_ID)
                .company(company())
                .municipality(municipality(MUNICIPALITY_ID))
                .name("Sede")
                .build();
    }

    private static LocationRequest request(Long municipalityId) {
        return new LocationRequest("Sede", "Rua A", municipalityId);
    }

    private static DataIntegrityViolationException integrityViolation(String sqlState, String constraint) {
        var cause = new ConstraintViolationException("violation", new SQLException("violation", sqlState), constraint);
        return new DataIntegrityViolationException("violation", cause);
    }
}
