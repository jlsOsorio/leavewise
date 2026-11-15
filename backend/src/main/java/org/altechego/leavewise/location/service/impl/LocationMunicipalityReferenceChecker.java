package org.altechego.leavewise.location.service.impl;

import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.municipality.service.MunicipalityReferenceChecker;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LocationMunicipalityReferenceChecker implements MunicipalityReferenceChecker {

    private final CompanyLocationRepository locationRepository;


    @Override
    public boolean isMunicipalityInUse(Long municipalityId) {
        return locationRepository.existsByMunicipalityId(municipalityId);
    }
}
