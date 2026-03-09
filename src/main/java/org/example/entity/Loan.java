package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.Banka;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    private Banka bankName;

    @Column(nullable = false)
    private BigDecimal loanAmount;

    @Column(nullable = false)
    private BigDecimal remainingDebt;

    @Column(nullable = false)
    private Integer installmentCount;

    @Column(nullable = false)
    private Integer paidInstallments;

    @Column(nullable = false)
    private BigDecimal monthlyPayment;

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean active = true;

    private LocalDateTime createdAt;

    private Integer paymentDay;
}
