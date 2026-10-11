package org.altechego.leavewise.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.user.entity.LwUser;
import org.altechego.leavewise.user.enums.Role;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final long COMPANY_ID = 3L;
    private static final long LOCATION_ID = 11L;

    @Mock
    private LwUserRepository lwUserRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyLocationRepository locationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;


    @Test
    void createAdminIfAbsent_emailNotTaken_shouldSaveActiveAdminWithHashedPasswordAndLowercasedEmail() {
        var company = Company.builder().id(COMPANY_ID).build();
        var location = CompanyLocation.builder().id(LOCATION_ID).build();
        when(lwUserRepository.findByEmail("admin@leavewise.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret-password")).thenReturn("{bcrypt}hash");
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(locationRepository.getReferenceById(LOCATION_ID)).thenReturn(location);
        when(lwUserRepository.save(any(LwUser.class))).thenAnswer(call -> call.getArgument(0));

        var created = userService.createAdminIfAbsent(
                " Admin@LeaveWise.local ", "secret-password", "Administrator", COMPANY_ID, LOCATION_ID);

        var saved = ArgumentCaptor.forClass(LwUser.class);
        verify(lwUserRepository).save(saved.capture());
        assertThat(created).isTrue();
        assertThat(saved.getValue())
                .extracting(LwUser::getEmail, LwUser::getPasswordHash, LwUser::getRole, LwUser::isActive, LwUser::getName)
                .containsExactly("admin@leavewise.local", "{bcrypt}hash", Role.ADMIN, true, "Administrator");
        assertThat(saved.getValue().getCompany()).isSameAs(company);
        assertThat(saved.getValue().getCompanyLocation()).isSameAs(location);
    }

    @Test
    void createAdminIfAbsent_adminExists_shouldLeaveItUnchanged() {
        var existing = LwUser.builder().id(1L).email("admin@leavewise.local").role(Role.ADMIN).passwordHash("old").build();
        when(lwUserRepository.findByEmail("admin@leavewise.local")).thenReturn(Optional.of(existing));

        var created = userService.createAdminIfAbsent(
                "admin@leavewise.local", "new-password", "Administrator", COMPANY_ID, LOCATION_ID);

        assertThat(created).isFalse();
        assertThat(existing.getPasswordHash()).isEqualTo("old");
        verify(lwUserRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void createAdminIfAbsent_emailBelongsToRegularUser_shouldNotPromoteIt() {
        var existing = LwUser.builder().id(2L).email("admin@leavewise.local").role(Role.USER).build();
        when(lwUserRepository.findByEmail("admin@leavewise.local")).thenReturn(Optional.of(existing));

        var created = userService.createAdminIfAbsent(
                "admin@leavewise.local", "new-password", "Administrator", COMPANY_ID, LOCATION_ID);

        assertThat(created).isFalse();
        assertThat(existing.getRole()).isEqualTo(Role.USER);
        verify(lwUserRepository, never()).save(any());
    }
}
