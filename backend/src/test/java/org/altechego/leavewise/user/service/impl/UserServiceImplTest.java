package org.altechego.leavewise.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.altechego.leavewise.common.exception.ApiException;
import org.altechego.leavewise.common.exception.ErrorCode;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.company.repository.CompanyRepository;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.location.repository.CompanyLocationRepository;
import org.altechego.leavewise.user.dto.UserRegistrationRequest;
import org.altechego.leavewise.user.entity.LwUser;
import org.altechego.leavewise.user.enums.Role;
import org.altechego.leavewise.user.mapper.UserMapper;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

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

    @Test
    void register_validRequest_shouldSaveActiveUserAndReturnIt() {
        stubValidCompanyAndLocation();
        stubReferences();
        when(lwUserRepository.existsByEmail("ana@example.pt")).thenReturn(false);
        when(lwUserRepository.existsByNif("123456789")).thenReturn(false);
        when(passwordEncoder.encode("secret-password")).thenReturn("{bcrypt}hash");
        when(lwUserRepository.save(any(LwUser.class))).thenAnswer(call -> {
            LwUser user = call.getArgument(0);
            user.setId(42L);
            return user;
        });

        var response = userService.register(request(" Ana@Example.PT ", "123456789"));

        var saved = ArgumentCaptor.forClass(LwUser.class);
        verify(lwUserRepository).save(saved.capture());
        assertThat(saved.getValue())
                .extracting(LwUser::getEmail, LwUser::getPasswordHash, LwUser::getRole, LwUser::isActive, LwUser::getNif)
                .containsExactly("ana@example.pt", "{bcrypt}hash", Role.USER, true, "123456789");
        assertThat(response)
                .extracting("id", "email", "role", "companyId", "companyLocationId")
                .containsExactly(42L, "ana@example.pt", Role.USER, COMPANY_ID, LOCATION_ID);
    }

    @Test
    void register_blankNifAndContact_shouldStoreNulls() {
        stubValidCompanyAndLocation();
        stubReferences();
        when(lwUserRepository.save(any(LwUser.class))).thenAnswer(call -> call.getArgument(0));

        userService.register(new UserRegistrationRequest(
                "Ana", "ana@example.pt", "secret-password", " ", "", COMPANY_ID, LOCATION_ID));

        var saved = ArgumentCaptor.forClass(LwUser.class);
        verify(lwUserRepository).save(saved.capture());
        assertThat(saved.getValue()).extracting(LwUser::getNif, LwUser::getContact).containsExactly(null, null);
        verify(lwUserRepository, never()).existsByNif(any());
    }

    @Test
    void register_unknownCompany_shouldThrowNotFound() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> userService.register(request("ana@example.pt", null)), ErrorCode.NOT_FOUND);
    }

    @Test
    void register_locationOfAnotherCompany_shouldThrowLocationCompanyMismatch() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(locationRepository.existsByIdAndCompanyId(LOCATION_ID, COMPANY_ID)).thenReturn(false);

        assertErrorCode(() -> userService.register(request("ana@example.pt", null)), ErrorCode.LOCATION_COMPANY_MISMATCH);
    }

    @Test
    void register_emailTakenInAnotherCase_shouldThrowEmailTaken() {
        stubValidCompanyAndLocation();
        when(lwUserRepository.existsByEmail("ana@example.pt")).thenReturn(true);

        assertErrorCode(() -> userService.register(request("ANA@example.pt", null)), ErrorCode.EMAIL_TAKEN);
    }

    @Test
    void register_nifTaken_shouldThrowNifTaken() {
        stubValidCompanyAndLocation();
        when(lwUserRepository.existsByNif("123456789")).thenReturn(true);

        assertErrorCode(() -> userService.register(request("ana@example.pt", "123456789")), ErrorCode.NIF_TAKEN);
    }


    private void stubValidCompanyAndLocation() {
        when(companyRepository.existsById(COMPANY_ID)).thenReturn(true);
        when(locationRepository.existsByIdAndCompanyId(LOCATION_ID, COMPANY_ID)).thenReturn(true);
    }

    private void stubReferences() {
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(Company.builder().id(COMPANY_ID).build());
        when(locationRepository.getReferenceById(LOCATION_ID)).thenReturn(CompanyLocation.builder().id(LOCATION_ID).build());
    }

    private static UserRegistrationRequest request(String email, String nif) {
        return new UserRegistrationRequest("Ana", email, "secret-password", nif, null, COMPANY_ID, LOCATION_ID);
    }

    private static void assertErrorCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(code);
    }
}
