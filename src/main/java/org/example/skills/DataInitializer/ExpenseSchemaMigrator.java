package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skills.enums.ExpenseType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateExpensePaymentMethod() {
        try {
            jdbcTemplate.execute("""
                UPDATE tbl_expenses
                SET payment_method = 'CASH'
                WHERE payment_method IS NULL
            """);
            log.info("Expense payment method defaults synchronized.");
        } catch (Exception e) {
            log.warn("Expense schema migration skipped: {}", e.getMessage());
        }
    }

    @PostConstruct
    public void migrateExpenseTypeCheckConstraint() {
        try {
            // Hibernate enum check constraint'leri (ddl-auto=update) enum listesi degisince otomatik guncellenmeyebiliyor.
            // Bu constraint eski kaldigi icin ARAC_GIDERLERI gibi yeni degerler insert'te patliyor.
            String allowed = Arrays.stream(ExpenseType.values())
                    .map(Enum::name)
                    .map(v -> "'" + v + "'")
                    .collect(Collectors.joining(","));

            jdbcTemplate.execute("ALTER TABLE tbl_expenses DROP CONSTRAINT IF EXISTS tbl_expenses_type_check");
            jdbcTemplate.execute("ALTER TABLE tbl_expenses ADD CONSTRAINT tbl_expenses_type_check CHECK (\"type\" IN (" + allowed + "))");

            log.info("Expense type check constraint synchronized.");
        } catch (Exception e) {
            log.warn("Expense type constraint migration skipped: {}", e.getMessage());
        }
    }
}
