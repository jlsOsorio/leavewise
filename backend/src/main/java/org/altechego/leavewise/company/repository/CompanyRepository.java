package org.altechego.leavewise.company.repository;

import java.util.Optional;

import org.altechego.leavewise.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);
}
