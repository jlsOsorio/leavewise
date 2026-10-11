package org.altechego.leavewise.company.entity;

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
import org.altechego.leavewise.common.entity.AuditableEntity;
import org.altechego.leavewise.company.constants.CompanyConstants;

@Entity
@Table(name = "company")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Company extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = CompanyConstants.NAME_MAX_LENGTH)
    private String name;


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Company company && id != null && id.equals(company.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
