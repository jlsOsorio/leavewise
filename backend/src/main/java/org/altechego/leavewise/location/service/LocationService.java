package org.altechego.leavewise.location.service;

import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.location.dto.LocationRequest;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface LocationService {

    LocationResponse findOrCreate(Long companyId, Long municipalityId, String name);

    PageResponse<LocationSummaryResponse> findByCompany(Long companyId, Pageable pageable);

    PageResponse<LocationResponse> findDetailsByCompany(Long companyId, Pageable pageable);

    LocationResponse findById(Long id);

    LocationResponse create(Long companyId, LocationRequest request);

    LocationResponse update(Long id, LocationRequest request);

    void delete(Long id);
}
