package org.altechego.leavewise.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.company.service.CompanyReferenceChecker;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserCompanyReferenceChecker implements CompanyReferenceChecker {

    private final LwUserRepository userRepository;


    @Override
    public boolean isCompanyInUse(Long companyId) {
        return userRepository.existsByCompanyId(companyId);
    }
}
