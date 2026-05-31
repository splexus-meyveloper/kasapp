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

    /**
     * islem_kodu sütununu nullable yap — custom işlem kodları için gerekli.
     * ddl-auto: update NOT NULL constraint'i otomatik kaldırmadığı için manuel yapıyoruz.
     */
    @PostConstruct
    public void migrateIslemKoduNullable() {
        try {
            jdbcTemplate.execute("""
                ALTER TABLE tbl_banka_islem
                ALTER COLUMN islem_kodu DROP NOT NULL
            """);
            log.info("tbl_banka_islem.islem_kodu set to nullable.");
        } catch (Exception e) {
            log.warn("islem_kodu nullable migration skipped (already nullable or table not found): {}", e.getMessage());
        }
    }
}
