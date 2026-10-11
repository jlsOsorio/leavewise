package org.altechego.leavewise.municipality.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.altechego.leavewise.municipality.constants.MunicipalityConstants;

// Unique on (lower(name), lower(district), lower(country)): an expression index in V1
// (uk_municipality_name_district_country), which @UniqueConstraint can't express
@Entity
@Table(name = "municipality")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Municipality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = MunicipalityConstants.NAME_MAX_LENGTH)
    private String name;

    @Column(name = "district", nullable = false, length = MunicipalityConstants.DISTRICT_MAX_LENGTH)
    private String district;

    @Column(name = "country", nullable = false, length = MunicipalityConstants.COUNTRY_MAX_LENGTH)
    private String country;


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Municipality municipality && id != null && id.equals(municipality.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
