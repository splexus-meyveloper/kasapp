package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BankaSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateBankaIslemConstraints() {
        try {
            jdbcTemplate.execute("""
                ALTER TABLE tbl_banka_islem
                DROP CONSTRAINT IF EXISTS tbl_banka_islem_islem_kodu_check
            """);
            log.info("Banka islem schema check constraint synchronized.");
        } catch (Exception e) {
            log.warn("Banka islem schema migration skipped: {}", e.getMessage());
        }
    }
}
