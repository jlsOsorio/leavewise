package org.altechego.leavewise.company.service.impl;

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
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.company.constants.CompanyConstants;
import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.mapper.CompanyMapper;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.company.service.CompanyReferenceChecker;
import org.hibernate.exception.ConstraintViolationException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    private static final long COMPANY_ID = 3L;
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String RESTRICT_VIOLATION = "23001";
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin@example.pt", Role.ADMIN, 9L);
    private static final AuthenticatedUser USER = new AuthenticatedUser(2L, "ana@example.pt", Role.USER, COMPANY_ID);

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyReferenceChecker referenceChecker;

    private CompanyServiceImpl companyService;


    @BeforeEach
    void setUp() {
        companyService = new CompanyServiceImpl(
                companyRepository, Mappers.getMapper(CompanyMapper.class), List.of(referenceChecker));
    }

    @Test
    void findOrCreate_companyExists_shouldReturnItWithoutSaving() {
        when(companyRepository.findByName("LeaveWise")).thenReturn(Optional.of(company()));

        var response = companyService.findOrCreate("LeaveWise");

        assertThat(response.id()).isEqualTo(COMPANY_ID);
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
        when(companyRepository.findAll(FIRST_PAGE)).thenReturn(new PageImpl<>(List.of(company()), FIRST_PAGE, 1));

        var page = companyService.search(query, FIRST_PAGE);

        assertThat(page.content()).containsExactly(new CompanyResponse(COMPANY_ID, "LeaveWise"));
        verify(companyRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void search_queryWithSpaces_shouldSearchTrimmedNameAndMapPage() {
        var pageable = PageRequest.of(1, 5);
        when(companyRepository.findByNameContainingIgnoreCase("leave", pageable))
                .thenReturn(new PageImpl<>(List.of(company()), pageable, 6));

        var page = companyService.search("  leave ", pageable);

        assertThat(page.content()).containsExactly(new CompanyResponse(COMPANY_ID, "LeaveWise"));
        assertThat(page).extracting("page", "size", "totalElements", "totalPages").containsExactly(1, 5, 6L, 2);
    }

    @Test
    void findById_userOwnCompany_shouldReturnIt() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company()));

        var response = companyService.findById(USER, COMPANY_ID);

        assertThat(response.name()).isEqualTo("LeaveWise");
    }

    @Test
    void findById_userAnotherCompany_shouldThrowNotFoundWithoutQuerying() {
        assertErrorCode(() -> companyService.findById(USER, 99L), ErrorCode.NOT_FOUND);
        verify(companyRepository, never()).findById(any());
    }

    @Test
    void findById_adminUnknownId_shouldThrowNotFound() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> companyService.findById(ADMIN, 99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void create_validRequest_shouldSaveMappedCompany() {
        when(companyRepository.saveAndFlush(any(Company.class))).thenAnswer(call -> call.getArgument(0));

        var response = companyService.create(new CompanyRequest("Acme"));

        assertThat(response.name()).isEqualTo("Acme");
    }

    @Test
    void create_uniqueNameViolated_shouldThrowDuplicate() {
        when(companyRepository.saveAndFlush(any(Company.class)))
                .thenThrow(integrityViolation(UNIQUE_VIOLATION, CompanyConstants.UNIQUE_CONSTRAINT));

        assertErrorCode(() -> companyService.create(new CompanyRequest("Acme")), ErrorCode.DUPLICATE);
    }

    @Test
    void create_otherIntegrityViolation_shouldRethrow() {
        var violation = integrityViolation(UNIQUE_VIOLATION, "some_other_constraint");
        when(companyRepository.saveAndFlush(any(Company.class))).thenThrow(violation);

        assertThatThrownBy(() -> companyService.create(new CompanyRequest("Acme"))).isSameAs(violation);
    }

    @Test
    void update_existingCompany_shouldRenameIt() {
        var existing = company();
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existing));
        when(companyRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = companyService.update(COMPANY_ID, new CompanyRequest("Acme"));

        assertThat(response).isEqualTo(new CompanyResponse(COMPANY_ID, "Acme"));
    }

    @Test
    void update_unknownId_shouldThrowNotFound() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> companyService.update(99L, new CompanyRequest("Acme")), ErrorCode.NOT_FOUND);
    }

    @Test
    void delete_unreferencedCompany_shouldDeleteIt() {
        var existing = company();
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(existing));

        companyService.delete(COMPANY_ID);

        verify(companyRepository).delete(existing);
        verify(companyRepository).flush();
    }

    @Test
    void delete_unknownId_shouldThrowNotFound() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertErrorCode(() -> companyService.delete(99L), ErrorCode.NOT_FOUND);
    }

    @Test
    void delete_referenced_shouldThrowResourceInUseWithoutDeleting() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company()));
        when(referenceChecker.isCompanyInUse(COMPANY_ID)).thenReturn(true);

        assertErrorCode(() -> companyService.delete(COMPANY_ID), ErrorCode.RESOURCE_IN_USE);
        verify(companyRepository, never()).delete(any());
    }

    @Test
    void delete_referencedAfterCheck_shouldThrowResourceInUse() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company()));
        doThrow(integrityViolation(RESTRICT_VIOLATION, "fk_lw_user_company")).when(companyRepository).flush();

        assertErrorCode(() -> companyService.delete(COMPANY_ID), ErrorCode.RESOURCE_IN_USE);
    }


    private static Company company() {
        return Company.builder().id(COMPANY_ID).name("LeaveWise").build();
    }

    private static DataIntegrityViolationException integrityViolation(String sqlState, String constraint) {
        var cause = new ConstraintViolationException("violation", new SQLException("violation", sqlState), constraint);
        return new DataIntegrityViolationException("violation", cause);
    }
}
