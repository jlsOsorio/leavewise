package org.altechego.leavewise.municipality.repository;

import java.util.Optional;

import org.altechego.leavewise.municipality.entity.Municipality;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {

    Optional<Municipality> findByNameIgnoreCaseAndDistrictIgnoreCaseAndCountryIgnoreCase(
            String name, String district, String country);
}
