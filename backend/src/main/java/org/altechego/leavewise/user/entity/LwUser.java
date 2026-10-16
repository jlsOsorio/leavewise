package org.altechego.leavewise.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.altechego.leavewise.common.entity.AuditableEntity;
import org.altechego.leavewise.common.security.Role;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.location.entity.CompanyLocation;
import org.altechego.leavewise.user.constants.UserConstants;

@Entity
@Table(name = "lw_user")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LwUser extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = UserConstants.NAME_MAX_LENGTH)
    private String name;

    @Column(name = "nif", unique = true, length = UserConstants.NIF_LENGTH)
    private String nif;

    @Column(name = "email", nullable = false, unique = true, length = UserConstants.EMAIL_MAX_LENGTH)
    private String email;

    @ToString.Exclude
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = UserConstants.ROLE_MAX_LENGTH)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "contact", length = UserConstants.CONTACT_MAX_LENGTH)
    private String contact;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Mapped on company_location_id alone; the composite FK (company_id, company_location_id) in V1
    // keeps the location inside the user's company
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_location_id", nullable = false)
    private CompanyLocation companyLocation;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LwUser user && id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
