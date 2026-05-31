package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.skills.enums.ExpensePaymentMethod;

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

    /** Masraf türü kodu — built-in enum adı veya özel kategori kodu (String olarak saklanır) */
    @Column(name = "type", length = 100)
    private String type;

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
