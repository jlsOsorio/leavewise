package org.altechego.leavewise.location.service.impl;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.common.exception.ConstraintViolations;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.exception.CompanyNotFoundException;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.constants.LocationConstants;
import org.altechego.leavewise.location.dto.LocationRequest;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.dto.LocationSummaryResponse;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.location.exception.DuplicateLocationException;
import org.altechego.leavewise.location.exception.LocationInUseException;
import org.altechego.leavewise.location.exception.LocationNotFoundException;
import org.altechego.leavewise.location.mapper.LocationMapper;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.location.service.LocationReferenceChecker;
import org.altechego.leavewise.location.service.LocationService;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.exception.MunicipalityNotFoundException;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final List<LocationReferenceChecker> referenceCheckers;


    @Override
    @Transactional
    public LocationResponse findOrCreate(Long companyId, Long municipalityId, String name) {
        var location = locationRepository.findByCompanyIdAndName(companyId, name)
                .orElseGet(() -> insertLocation(companyId, municipalityId, name));
        return locationMapper.toResponse(location);
    }

    // Unknown company is 404, not an empty page, so a stale company id is not mistaken for "no locations"
    @Override
    public PageResponse<LocationSummaryResponse> findByCompany(Long companyId, Pageable pageable) {
        requireCompany(companyId);
        var locations = locationRepository.findByCompanyId(companyId, pageable);
        return PageResponse.from(locations.map(locationMapper::toSummaryResponse));
    }

    @Override
    public PageResponse<LocationResponse> findDetailsByCompany(Long companyId, Pageable pageable) {
        requireCompany(companyId);
        var locations = locationRepository.findWithMunicipalityByCompanyId(companyId, pageable);
        return PageResponse.from(locations.map(locationMapper::toResponse));
    }

    @Override
    public LocationResponse findById(Long id) {
        return locationMapper.toResponse(getLocationWithMunicipality(id));
    }

    @Override
    @Transactional
    public LocationResponse create(Long companyId, LocationRequest request) {
        var location = locationMapper.toEntity(request);
        location.setCompany(getCompanyReference(companyId));
        location.setMunicipality(getMunicipality(request.municipalityId()));

        var created = saveUnique(location);
        log.info("Created location {} for company {}", created.getId(), companyId);
        return locationMapper.toResponse(created);
    }

    @Override
    @Transactional
    public LocationResponse update(Long id, LocationRequest request) {
        var location = getLocationWithMunicipality(id);
        locationMapper.update(request, location);
        if (!location.getMunicipality().getId().equals(request.municipalityId())) {
            location.setMunicipality(getMunicipality(request.municipalityId()));
        }

        var updated = saveUnique(location);
        log.info("Updated location {}", id);
        return locationMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var location = locationRepository.findById(id).orElseThrow(() -> new LocationNotFoundException(id));
        if (referenceCheckers.stream().anyMatch(checker -> checker.isLocationInUse(id))) {
            throw new LocationInUseException(id);
        }

        try {
            locationRepository.delete(location);
            locationRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.isForeignKeyViolation(ex)) {
                throw new LocationInUseException(id);
            }
            throw ex;
        }
        log.info("Deleted location {}", id);
    }

    private void requireCompany(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
    }

    private Company getCompanyReference(Long companyId) {
        requireCompany(companyId);
        return companyRepository.getReferenceById(companyId);
    }

    private Municipality getMunicipality(Long municipalityId) {
        return municipalityRepository.findById(municipalityId)
                .orElseThrow(() -> new MunicipalityNotFoundException(municipalityId));
    }

    private CompanyLocation getLocationWithMunicipality(Long id) {
        return locationRepository.findWithMunicipalityById(id).orElseThrow(() -> new LocationNotFoundException(id));
    }

    private CompanyLocation saveUnique(CompanyLocation location) {
        try {
            return locationRepository.saveAndFlush(location);
        } catch (DataIntegrityViolationException ex) {
            if (LocationConstants.UNIQUE_CONSTRAINT.equals(ConstraintViolations.nameOf(ex))) {
                throw new DuplicateLocationException();
            }
            throw ex;
        }
    }

    private CompanyLocation insertLocation(Long companyId, Long municipalityId, String name) {
        var location = locationRepository.save(CompanyLocation.builder()
                .company(companyRepository.getReferenceById(companyId))
                .municipality(municipalityRepository.getReferenceById(municipalityId))
                .name(name)
                .build());
        log.info("Created location {} for company {}", name, companyId);
        return location;
    }
}
