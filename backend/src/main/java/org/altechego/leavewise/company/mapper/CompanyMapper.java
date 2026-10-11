package org.altechego.leavewise.company.mapper;

import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.entity.Company;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyResponse toResponse(Company company);
}
