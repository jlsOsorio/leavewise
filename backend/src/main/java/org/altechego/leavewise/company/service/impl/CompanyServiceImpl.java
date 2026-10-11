package org.altechego.leavewise.company.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.mapper.CompanyMapper;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;


    @Override
    @Transactional
    public CompanyResponse findOrCreate(String name) {
        var company = companyRepository.findByName(name).orElseGet(() -> create(name));
        return companyMapper.toResponse(company);
    }

    private Company create(String name) {
        var company = companyRepository.save(Company.builder().name(name).build());
        log.info("Created company {}", name);
        return company;
    }
}
