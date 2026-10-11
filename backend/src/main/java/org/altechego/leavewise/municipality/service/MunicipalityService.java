package org.altechego.leavewise.municipality.service;

import org.altechego.leavewise.municipality.dto.MunicipalityResponse;

public interface MunicipalityService {

    MunicipalityResponse findOrCreate(String name, String district, String country);
}
