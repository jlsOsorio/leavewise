package org.altechego.leavewise.municipality.service;

import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.municipality.dto.MunicipalityRequest;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.springframework.data.domain.Pageable;

public interface MunicipalityService {

    MunicipalityResponse findOrCreate(String name, String district, String country);

    PageResponse<MunicipalityResponse> search(String nameQuery, Pageable pageable);

    MunicipalityResponse findById(Long id);

    MunicipalityResponse create(MunicipalityRequest request);

    MunicipalityResponse update(Long id, MunicipalityRequest request);

    void delete(Long id);
}
