package org.altechego.leavewise.location.mapper;

import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "municipalityId", source = "municipality.id")
    LocationResponse toResponse(CompanyLocation location);
}
