package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.PriceCalcRunStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_price_calc_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalcRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    private Long companyId;

    private Long triggeredBy;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriceCalcRunStatus status;

    private int totalMatched;

    private int totalUnmatched;
}
