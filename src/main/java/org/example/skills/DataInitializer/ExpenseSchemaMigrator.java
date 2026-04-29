package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
}
