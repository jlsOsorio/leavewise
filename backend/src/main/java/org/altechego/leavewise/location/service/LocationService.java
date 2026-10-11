package org.altechego.leavewise.location.service;

import org.altechego.leavewise.location.dto.LocationResponse;

public interface LocationService {

    LocationResponse findOrCreate(Long companyId, Long municipalityId, String name);
}
