package org.altechego.leavewise.schedule.entity;

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

@Entity
@Table(
        name = "work_schedule_day",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_schedule_day",
                columnNames = {"work_schedule_id", "day_of_week"}))
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkScheduleDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_schedule_id", nullable = false)
    private WorkSchedule schedule;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "is_working_day", nullable = false)
    private boolean workingDay;


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof WorkScheduleDay day && id != null && id.equals(day.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
