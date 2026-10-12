package org.altechego.leavewise.user.repository;

import java.util.Optional;

import org.altechego.leavewise.user.entity.LwUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LwUserRepository extends JpaRepository<LwUser, Long> {

    Optional<LwUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNif(String nif);
}
