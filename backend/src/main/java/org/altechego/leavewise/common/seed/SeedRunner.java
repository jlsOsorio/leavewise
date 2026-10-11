package org.altechego.leavewise.common.seed;

import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.common.config.SeedProperties;
import org.altechego.leavewise.company.service.CompanyService;
import org.altechego.leavewise.location.service.LocationService;
import org.altechego.leavewise.municipality.service.MunicipalityService;
import org.altechego.leavewise.user.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeedRunner implements ApplicationRunner {

    private final SeedProperties seed;
    private final MunicipalityService municipalityService;
    private final CompanyService companyService;
    private final LocationService locationService;
    private final UserService userService;


    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var municipality = municipalityService.findOrCreate(seed.municipality(), seed.district(), seed.country());
        var company = companyService.findOrCreate(seed.companyName());
        var location = locationService.findOrCreate(company.id(), municipality.id(), seed.locationName());

        userService.createAdminIfAbsent(
                seed.adminEmail(), seed.adminPassword(), seed.adminName(), company.id(), location.id());
    }
}
