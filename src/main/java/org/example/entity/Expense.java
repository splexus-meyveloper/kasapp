package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.ExpensePaymentMethod;
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

    @Enumerated(EnumType.STRING)
    private ExpensePaymentMethod paymentMethod;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String description;

    private LocalDate expenseDate;

    private LocalDateTime createdAt;

    private Long createdBy;

    @PrePersist
    @PreUpdate
    private void validateRequiredFields() {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Masraf aciklamasi bos olamaz.");
        }

        String normalized = description.trim();
        if (normalized.equalsIgnoreCase("null") || normalized.equalsIgnoreCase("undefined")) {
            throw new IllegalArgumentException("Masraf aciklamasi bos olamaz.");
        }

        description = normalized;
    }
}
