package org.altechego.leavewise.location.repository;

import java.util.Optional;

import org.altechego.leavewise.location.entity.CompanyLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, Long> {

    Optional<CompanyLocation> findByCompanyIdAndName(Long companyId, String name);
}
