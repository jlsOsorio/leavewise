package org.altechego.leavewise.schedule.repository;

import java.util.Optional;

import org.altechego.leavewise.schedule.entity.WorkSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    @EntityGraph(attributePaths = "days")
    Optional<WorkSchedule> findWithDaysByCompanyId(Long companyId);
}
