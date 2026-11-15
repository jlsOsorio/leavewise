package org.altechego.leavewise.company.mapper;

import org.altechego.leavewise.company.dto.CompanyRequest;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyResponse toResponse(Company company);

    @Mapping(target = "id", ignore = true)
    Company toEntity(CompanyRequest request);

    @Mapping(target = "id", ignore = true)
    void update(CompanyRequest request, @MappingTarget Company company);
}
