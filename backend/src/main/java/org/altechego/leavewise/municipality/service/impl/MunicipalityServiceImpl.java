package org.altechego.leavewise.municipality.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.common.exception.ConstraintViolations;
import org.altechego.leavewise.common.web.PageResponse;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;
import org.altechego.leavewise.municipality.dto.MunicipalityRequest;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.exception.DuplicateMunicipalityException;
import org.altechego.leavewise.municipality.exception.MunicipalityInUseException;
import org.altechego.leavewise.municipality.exception.MunicipalityNotFoundException;
import org.altechego.leavewise.municipality.mapper.MunicipalityMapper;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.altechego.leavewise.municipality.service.MunicipalityService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MunicipalityServiceImpl implements MunicipalityService {

    private final MunicipalityRepository municipalityRepository;
    private final MunicipalityMapper municipalityMapper;
    private final CompanyLocationRepository locationRepository;


    @Override
    @Transactional
    public MunicipalityResponse findOrCreate(String name, String district, String country) {
        var municipality = municipalityRepository
                .findByNameIgnoreCaseAndDistrictIgnoreCaseAndCountryIgnoreCase(name, district, country)
                .orElseGet(() -> insertMunicipality(name, district, country));
        return municipalityMapper.toResponse(municipality);
    }

    @Override
    public PageResponse<MunicipalityResponse> search(String nameQuery, Pageable pageable) {
        var municipalities = nameQuery == null || nameQuery.isBlank()
                ? municipalityRepository.findAll(pageable)
                : municipalityRepository.findByNameContainingIgnoreCase(nameQuery.trim(), pageable);
        return PageResponse.from(municipalities.map(municipalityMapper::toResponse));
    }

    @Override
    public MunicipalityResponse findById(Long id) {
        return municipalityMapper.toResponse(getMunicipality(id));
    }

    @Override
    @Transactional
    public MunicipalityResponse create(MunicipalityRequest request) {
        var municipality = saveUnique(municipalityMapper.toEntity(request));
        log.info("Created municipality {}", municipality.getId());
        return municipalityMapper.toResponse(municipality);
    }

    @Override
    @Transactional
    public MunicipalityResponse update(Long id, MunicipalityRequest request) {
        var municipality = getMunicipality(id);
        municipalityMapper.update(request, municipality);

        var updated = saveUnique(municipality);
        log.info("Updated municipality {}", id);
        return municipalityMapper.toResponse(updated);
    }

    // BR-15: checked up front; the RESTRICT foreign keys catch a reference added concurrently
    @Override
    @Transactional
    public void delete(Long id) {
        var municipality = getMunicipality(id);
        if (locationRepository.existsByMunicipalityId(id)) {
            throw new MunicipalityInUseException(id);
        }

        try {
            municipalityRepository.delete(municipality);
            municipalityRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.isForeignKeyViolation(ex)) {
                throw new MunicipalityInUseException(id);
            }
            throw ex;
        }
        log.info("Deleted municipality {}", id);
    }

    private Municipality getMunicipality(Long id) {
        return municipalityRepository.findById(id).orElseThrow(() -> new MunicipalityNotFoundException(id));
    }

    private Municipality saveUnique(Municipality municipality) {
        try {
            return municipalityRepository.saveAndFlush(municipality);
        } catch (DataIntegrityViolationException ex) {
            if (MunicipalityConstants.UNIQUE_CONSTRAINT.equals(ConstraintViolations.nameOf(ex))) {
                throw new DuplicateMunicipalityException();
            }
            throw ex;
        }
    }

    private Municipality insertMunicipality(String name, String district, String country) {
        var municipality = municipalityRepository.save(
                Municipality.builder().name(name).district(district).country(country).build());
        log.info("Created municipality {} ({}, {})", name, district, country);
        return municipality;
    }
}
