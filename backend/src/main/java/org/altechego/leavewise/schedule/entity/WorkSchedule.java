package org.altechego.leavewise.schedule.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.altechego.leavewise.company.entity.Company;
import org.altechego.leavewise.schedule.constants.WorkScheduleConstants;

@Entity
@Table(name = "work_schedule")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "name", nullable = false, length = WorkScheduleConstants.NAME_MAX_LENGTH)
    private String name;

    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkScheduleDay> days = new ArrayList<>();


    public void addDay(WorkScheduleDay day) {
        day.setSchedule(this);
        days.add(day);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof WorkSchedule schedule && id != null && id.equals(schedule.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
