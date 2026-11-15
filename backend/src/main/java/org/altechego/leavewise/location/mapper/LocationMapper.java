package org.altechego.leavewise.location.mapper;

import org.altechego.leavewise.location.dto.LocationRequest;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "companyId", source = "company.id")
    LocationResponse toResponse(CompanyLocation location);

    LocationSummaryResponse toSummaryResponse(CompanyLocation location);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "municipality", ignore = true)
    CompanyLocation toEntity(LocationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "municipality", ignore = true)
    void update(LocationRequest request, @MappingTarget CompanyLocation location);
}
