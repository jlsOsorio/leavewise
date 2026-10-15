package org.altechego.leavewise.location.service.impl;

import static org.altechego.leavewise.common.exception.ApiExceptionAssertions.assertErrorCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.location.mapper.LocationMapper;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    private static final long COMPANY_ID = 3L;
    private static final long MUNICIPALITY_ID = 7L;

    @Mock
    private CompanyLocationRepository locationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MunicipalityRepository municipalityRepository;

    private LocationServiceImpl locationService;


    @BeforeEach
    void setUp() {
        locationService = new LocationServiceImpl(
                locationRepository, companyRepository, municipalityRepository, Mappers.getMapper(LocationMapper.class));
    }

    @Test
    void findOrCreate_locationExists_shouldReturnItWithoutSaving() {
        var existing = CompanyLocation.builder()
                .id(11L)
                .company(Company.builder().id(COMPANY_ID).build())
                .municipality(Municipality.builder().id(MUNICIPALITY_ID).build())
                .name("Sede")
                .build();
        when(locationRepository.findByCompanyIdAndName(COMPANY_ID, "Sede")).thenReturn(Optional.of(existing));

        var response = locationService.findOrCreate(COMPANY_ID, MUNICIPALITY_ID, "Sede");

        assertThat(response.id()).isEqualTo(11L);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void findOrCreate_locationMissing_shouldSaveItInTheCompanyAndMunicipality() {
        var company = Company.builder().id(COMPANY_ID).build();
        var municipality = Municipality.builder().id(MUNICIPALITY_ID).build();
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
        assertThat(response.municipalityId()).isEqualTo(MUNICIPALITY_ID);
        assertThat(response.name()).isEqualTo("Sede");
    }

    @Test
    void findByCompany_existingCompany_shouldReturnPageOfLocationSummaries() {
        var pageable = PageRequest.of(0, 20);
        var location = CompanyLocation.builder().id(11L).name("Sede").build();
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(locationRepository.findByCompanyId(COMPANY_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(location), pageable, 1));

        var page = locationService.findByCompany(COMPANY_ID, pageable);

        assertThat(page.content()).containsExactly(new LocationSummaryResponse(11L, "Sede"));
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void findByCompany_unknownCompany_shouldThrowNotFound() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> locationService.findByCompany(COMPANY_ID, PageRequest.of(0, 20)), ErrorCode.NOT_FOUND);
        verify(locationRepository, never()).findByCompanyId(any(), any());
    }
}
