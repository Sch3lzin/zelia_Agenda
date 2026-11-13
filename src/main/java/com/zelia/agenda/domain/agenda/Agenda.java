package com.zelia.agenda.domain.agenda;

import com.zelia.agenda.domain.schoolClass.SchoolClass;
import com.zelia.agenda.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "agendas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_agenda_user_class_period_time_date",
                columnNames = {"user_id", "school_class_id", "period", "class_time", "date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassTime classTime;

    @Column(nullable = false)
    private LocalDate date;

    public Agenda(User user, SchoolClass schoolClass, @NotBlank String observation, @NotNull Period period, @NotNull ClassTime classTime, @NotNull LocalDate date) {
        this.user = user;
        this.schoolClass = schoolClass;
        this.observation = observation;
        this.period = period;
        this.classTime = classTime;
        this.date = date;
    }

    public Agenda update(SchoolClass schoolClass, String observation, @NotNull Period period, @NotNull ClassTime classTime, @NotNull LocalDate date) {
        this.schoolClass = schoolClass;
        this.observation = observation;
        this.period = period;
        this.classTime = classTime;
        this.date = date;
        return this;
    }
}
