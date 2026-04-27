package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_calendar_notes",
        indexes = {
                @Index(name = "idx_calendar_note_company_user", columnList = "companyId, userId"),
                @Index(name = "idx_calendar_note_date", columnList = "date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CalendarNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}