package org.altechego.leavewise.location.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.repository.CompanyRepository;
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
}
