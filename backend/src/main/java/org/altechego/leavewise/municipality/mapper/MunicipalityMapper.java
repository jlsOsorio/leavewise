package org.altechego.leavewise.municipality.mapper;

import org.altechego.leavewise.municipality.dto.MunicipalityRequest;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MunicipalityMapper {

    MunicipalityResponse toResponse(Municipality municipality);

    @Mapping(target = "id", ignore = true)
    Municipality toEntity(MunicipalityRequest request);

    @Mapping(target = "id", ignore = true)
    void update(MunicipalityRequest request, @MappingTarget Municipality municipality);
}
