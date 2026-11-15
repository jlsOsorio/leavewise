package org.altechego.leavewise.company.service;

import org.altechego.leavewise.common.security.AuthenticatedUser;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    CompanyResponse findOrCreate(String name);

    PageResponse<CompanyResponse> search(String nameQuery, Pageable pageable);

    CompanyResponse findById(AuthenticatedUser actor, Long id);

    CompanyResponse create(CompanyRequest request);

    CompanyResponse update(Long id, CompanyRequest request);

    void delete(Long id);
}
