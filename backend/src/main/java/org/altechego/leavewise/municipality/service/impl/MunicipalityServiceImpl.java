package org.altechego.leavewise.municipality.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.mapper.MunicipalityMapper;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.altechego.leavewise.municipality.service.MunicipalityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MunicipalityServiceImpl implements MunicipalityService {

    private final MunicipalityRepository municipalityRepository;
    private final MunicipalityMapper municipalityMapper;


    @Override
    @Transactional
    public MunicipalityResponse findOrCreate(String name, String district, String country) {
        var municipality = municipalityRepository
                .findByNameIgnoreCaseAndDistrictIgnoreCaseAndCountryIgnoreCase(name, district, country)
                .orElseGet(() -> create(name, district, country));
        return municipalityMapper.toResponse(municipality);
    }

    private Municipality create(String name, String district, String country) {
        var municipality = municipalityRepository.save(
                Municipality.builder().name(name).district(district).country(country).build());
        log.info("Created municipality {} ({}, {})", name, district, country);
        return municipality;
    }
}
