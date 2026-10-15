package org.altechego.leavewise.location.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.company.exception.CompanyNotFoundException;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.location.mapper.LocationMapper;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.location.service.LocationService;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final CompanyLocationRepository locationRepository;
    private final CompanyRepository companyRepository;
    private final MunicipalityRepository municipalityRepository;
    private final LocationMapper locationMapper;


    @Override
    @Transactional
    public LocationResponse findOrCreate(Long companyId, Long municipalityId, String name) {
        var location = locationRepository.findByCompanyIdAndName(companyId, name)
                .orElseGet(() -> create(companyId, municipalityId, name));
        return locationMapper.toResponse(location);
    }

    // Unknown company is 404, not an empty page, so a stale company id is not mistaken for "no locations"
    @Override
    public PageResponse<LocationSummaryResponse> findByCompany(Long companyId, Pageable pageable) {
        if (!companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
        var locations = locationRepository.findByCompanyId(companyId, pageable);
        return PageResponse.from(locations.map(locationMapper::toSummaryResponse));
    }

    private CompanyLocation create(Long companyId, Long municipalityId, String name) {
        var location = locationRepository.save(CompanyLocation.builder()
                .company(companyRepository.getReferenceById(companyId))
                .municipality(municipalityRepository.getReferenceById(municipalityId))
                .name(name)
                .build());
        log.info("Created location {} for company {}", name, companyId);
        return location;
    }
}
