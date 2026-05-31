package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_expense_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kod — Expense.type alanında saklanan değer (örn: "ELEKTRIK", "OZEL_GIDER") */
    @Column(nullable = false, length = 100)
    private String code;

    /** Kullanıcıya gösterilen isim */
    @Column(nullable = false, length = 150)
    private String label;

    /**
     * null  → tüm şirketlerin kullanabileceği sabit/built-in kategori
     * set   → sadece bu şirkete ait özel kategori
     */
    private Long companyId;
}
