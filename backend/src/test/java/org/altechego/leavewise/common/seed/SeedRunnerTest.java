package org.altechego.leavewise.common.seed;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.altechego.leavewise.common.config.SeedProperties;
import org.altechego.leavewise.company.dto.CompanyResponse;
import org.altechego.leavewise.company.service.CompanyService;
import org.altechego.leavewise.location.dto.LocationResponse;
import org.altechego.leavewise.location.service.LocationService;
import org.altechego.leavewise.municipality.dto.MunicipalityResponse;
import org.altechego.leavewise.municipality.service.MunicipalityService;
import org.altechego.leavewise.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeedRunnerTest {

    @Mock
    private MunicipalityService municipalityService;

    @Mock
    private CompanyService companyService;

    @Mock
    private LocationService locationService;

    @Mock
    private UserService userService;


    @Test
    void run_blankCountry_shouldSeedMunicipalityInPortugalThenCompanyLocationAndAdmin() {
        var seed = new SeedProperties(
                "admin@leavewise.local", "change-me", "Administrator", "LeaveWise", "Sede", "Lisboa", "Lisboa", " ");
        var runner = new SeedRunner(seed, municipalityService, companyService, locationService, userService);
        when(municipalityService.findOrCreate("Lisboa", "Lisboa", "Portugal"))
                .thenReturn(new MunicipalityResponse(7L, "Lisboa", "Lisboa", "Portugal"));
        when(companyService.findOrCreate("LeaveWise")).thenReturn(new CompanyResponse(3L, "LeaveWise"));
        when(locationService.findOrCreate(3L, 7L, "Sede")).thenReturn(new LocationResponse(11L, 3L, 7L, "Sede", null));

        runner.run(null);

        var order = inOrder(municipalityService, companyService, locationService, userService);
        order.verify(municipalityService).findOrCreate("Lisboa", "Lisboa", "Portugal");
        order.verify(companyService).findOrCreate("LeaveWise");
        order.verify(locationService).findOrCreate(3L, 7L, "Sede");
        order.verify(userService).createAdminIfAbsent("admin@leavewise.local", "change-me", "Administrator", 3L, 11L);
    }
}
