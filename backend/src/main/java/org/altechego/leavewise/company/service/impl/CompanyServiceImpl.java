package org.altechego.leavewise.company.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.common.exception.ConstraintViolations;
import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.company.constants.CompanyConstants;
import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.exception.CompanyInUseException;
import org.altechego.leavewise.company.exception.CompanyNotFoundException;
import org.altechego.leavewise.company.exception.DuplicateCompanyException;
import org.altechego.leavewise.company.mapper.CompanyMapper;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.company.service.CompanyReferenceChecker;
import org.altechego.leavewise.company.service.CompanyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final List<CompanyReferenceChecker> referenceCheckers;


    @Override
    @Transactional
    public CompanyResponse findOrCreate(String name) {
        var company = companyRepository.findByName(name).orElseGet(() -> insertCompany(name));
        return companyMapper.toResponse(company);
    }

    @Override
    public PageResponse<CompanyResponse> search(String nameQuery, Pageable pageable) {
        var companies = nameQuery == null || nameQuery.isBlank()
                ? companyRepository.findAll(pageable)
                : companyRepository.findByNameContainingIgnoreCase(nameQuery.trim(), pageable);
        return PageResponse.from(companies.map(companyMapper::toResponse));
    }

    @Override
    public CompanyResponse findById(AuthenticatedUser actor, Long id) {
        if (!isAdmin(actor) && !id.equals(actor.companyId())) {
            throw new CompanyNotFoundException(id);
        }
        return companyMapper.toResponse(getCompany(id));
    }

    @Override
    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        var company = saveUnique(companyMapper.toEntity(request));
        log.info("Created company {}", company.getId());
        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        var company = getCompany(id);
        companyMapper.update(request, company);

        var updated = saveUnique(company);
        log.info("Updated company {}", id);
        return companyMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var company = getCompany(id);
        if (referenceCheckers.stream().anyMatch(checker -> checker.isCompanyInUse(id))) {
            throw new CompanyInUseException(id);
        }

        try {
            companyRepository.delete(company);
            companyRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.isForeignKeyViolation(ex)) {
                throw new CompanyInUseException(id);
            }
            throw ex;
        }
        log.info("Deleted company {}", id);
    }

    private Company getCompany(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
    }

    private Company saveUnique(Company company) {
        try {
            return companyRepository.saveAndFlush(company);
        } catch (DataIntegrityViolationException ex) {
            if (CompanyConstants.UNIQUE_CONSTRAINT.equals(ConstraintViolations.nameOf(ex))) {
                throw new DuplicateCompanyException();
            }
            throw ex;
        }
    }

    private Company insertCompany(String name) {
        var company = companyRepository.save(Company.builder().name(name).build());
        log.info("Created company {}", name);
        return company;
    }

    private static boolean isAdmin(AuthenticatedUser actor) {
        return actor.role() == Role.ADMIN;
    }
}
