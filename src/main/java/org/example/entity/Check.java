package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.Banka;
import org.example.skills.enums.CheckStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="tbl_checks",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"checkNo","companyId"}
        ))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String checkNo;

    @Enumerated(EnumType.STRING)
    private Banka bank;

    private LocalDate dueDate;

    private BigDecimal amount;

    private String description;

    @Enumerated(EnumType.STRING)
    private CheckStatus status;

    private Long companyId;

    private Long createdBy;

    private LocalDateTime createdAt;
}
