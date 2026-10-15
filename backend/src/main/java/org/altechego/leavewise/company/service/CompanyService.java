package org.altechego.leavewise.company.service;

import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    CompanyResponse findOrCreate(String name);

    PageResponse<CompanyResponse> search(String nameQuery, Pageable pageable);
}
