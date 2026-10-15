package org.altechego.leavewise.company.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.mapper.CompanyMapper;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    private CompanyServiceImpl companyService;


    @BeforeEach
    void setUp() {
        companyService = new CompanyServiceImpl(companyRepository, Mappers.getMapper(CompanyMapper.class));
    }

    @Test
    void findOrCreate_companyExists_shouldReturnItWithoutSaving() {
        when(companyRepository.findByName("LeaveWise"))
                .thenReturn(Optional.of(Company.builder().id(3L).name("LeaveWise").build()));

        var response = companyService.findOrCreate("LeaveWise");

        assertThat(response.id()).isEqualTo(3L);
        verify(companyRepository, never()).save(any());
    }

    @Test
    void findOrCreate_companyMissing_shouldSaveAndReturnIt() {
        when(companyRepository.findByName("LeaveWise")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));

        var response = companyService.findOrCreate("LeaveWise");

        var saved = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("LeaveWise");
        assertThat(response.name()).isEqualTo("LeaveWise");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void search_blankQuery_shouldListAllCompanies(String query) {
        var pageable = PageRequest.of(0, 20);
        when(companyRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(company()), pageable, 1));

        var page = companyService.search(query, pageable);

        assertThat(page.content()).containsExactly(new CompanyResponse(3L, "LeaveWise"));
        verify(companyRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void search_queryWithSpaces_shouldSearchTrimmedNameAndMapPage() {
        var pageable = PageRequest.of(1, 5);
        when(companyRepository.findByNameContainingIgnoreCase("leave", pageable))
                .thenReturn(new PageImpl<>(List.of(company()), pageable, 6));

        var page = companyService.search("  leave ", pageable);

        assertThat(page.content()).containsExactly(new CompanyResponse(3L, "LeaveWise"));
        assertThat(page).extracting("page", "size", "totalElements", "totalPages").containsExactly(1, 5, 6L, 2);
    }


    private static Company company() {
        return Company.builder().id(3L).name("LeaveWise").build();
    }
}
