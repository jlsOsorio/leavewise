package org.altechego.leavewise.common.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.altechego.leavewise.TestcontainersConfiguration;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.municipality.entity.Municipality;
import org.altechego.leavewise.municipality.repository.MunicipalityRepository;
import org.altechego.leavewise.user.enums.Role;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SeedRunnerIT {

    private static final String ADMIN_EMAIL = "admin@leavewise.test";

    @Autowired
    private SeedRunner seedRunner;

    @Autowired
    private LwUserRepository lwUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyLocationRepository locationRepository;

    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    void startup_shouldSeedActiveAdminInSeededCompanyLocationAndMunicipality() {
        var admin = lwUserRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.getCreatedAt()).isNotNull();
        assertThat(admin.getUpdatedAt()).isNull();
        assertThat(passwordEncoder.matches("test-password", admin.getPasswordHash())).isTrue();
        assertThat(admin.getCompany().getName()).isEqualTo("Test Company");

        var location = admin.getCompanyLocation();
        assertThat(location.getName()).isEqualTo("Test Office");
        assertThat(location.getMunicipality())
                .extracting(Municipality::getName, Municipality::getDistrict, Municipality::getCountry)
                .containsExactly("Porto", "Porto", "Portugal");
    }

    @Test
    void run_calledAgain_shouldNotDuplicateAnything() {
        var users = lwUserRepository.count();
        var companies = companyRepository.count();
        var locations = locationRepository.count();
        var municipalities = municipalityRepository.count();

        seedRunner.run(null);

        assertThat(lwUserRepository.count()).isEqualTo(users);
        assertThat(companyRepository.count()).isEqualTo(companies);
        assertThat(locationRepository.count()).isEqualTo(locations);
        assertThat(municipalityRepository.count()).isEqualTo(municipalities);
    }
}
