package org.altechego.leavewise.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.altechego.leavewise.location.service.LocationReferenceChecker;
import org.altechego.leavewise.user.repository.LwUserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserLocationReferenceChecker implements LocationReferenceChecker {

    private final LwUserRepository userRepository;


    @Override
    public boolean isLocationInUse(Long locationId) {
        return userRepository.existsByCompanyLocationId(locationId);
    }
}
