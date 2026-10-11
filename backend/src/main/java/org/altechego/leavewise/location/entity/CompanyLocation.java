package org.altechego.leavewise.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.location.constants.LocationConstants;
import org.altechego.leavewise.municipality.entity.Municipality;

@Entity
// (company_id, id) is redundant as a key; it exists only as the target of the composite FK from lw_user
@Table(
        name = "company_location",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_company_location_company_name", columnNames = {"company_id", "name"}),
            @UniqueConstraint(name = "uk_company_location_company_id", columnNames = {"company_id", "id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompanyLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipality_id", nullable = false)
    private Municipality municipality;

    @Column(name = "name", nullable = false, length = LocationConstants.NAME_MAX_LENGTH)
    private String name;

    @Column(name = "address", length = LocationConstants.ADDRESS_MAX_LENGTH)
    private String address;


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof CompanyLocation location && id != null && id.equals(location.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
