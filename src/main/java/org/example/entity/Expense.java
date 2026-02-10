package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.ExpenseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_expenses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    private ExpenseType type;

    private BigDecimal amount;

    private String description;

    private LocalDate expenseDate;

    private LocalDateTime createdAt;

    private Long createdBy;
}
