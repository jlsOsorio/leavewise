package org.altechego.leavewise.municipality.mapper;

import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MunicipalityMapper {

    MunicipalityResponse toResponse(Municipality municipality);
}
