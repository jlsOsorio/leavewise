package org.altechego.leavewise.company.service;

import org.altechego.leavewise.company.dto.CompanyResponse;

public interface CompanyService {

    CompanyResponse findOrCreate(String name);
}
