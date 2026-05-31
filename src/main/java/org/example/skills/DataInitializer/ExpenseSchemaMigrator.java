package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    /** Built-in enum → label eşleştirmeleri */
    private static final Map<String, String> BUILTIN_LABELS = new LinkedHashMap<>();

    static {
        BUILTIN_LABELS.put("ELEKTRIK",           "ELEKTRİK");
        BUILTIN_LABELS.put("SU",                 "SU");
        BUILTIN_LABELS.put("ILETISIM",           "İLETİŞİM");
        BUILTIN_LABELS.put("KARGO_NAKLIYE",      "KARGO-NAKLİYE");
        BUILTIN_LABELS.put("ARAC_GIDERLERI",     "ARAÇ GİDERLERİ");
        BUILTIN_LABELS.put("IS_YERI",            "İŞ YERİ");
        BUILTIN_LABELS.put("ATOLYE",             "ATÖLYE");
        BUILTIN_LABELS.put("YEMEK",              "YEMEK");
        BUILTIN_LABELS.put("MARKET",             "MARKET");
        BUILTIN_LABELS.put("KIRTASIYE",          "KIRTASİYE");
        BUILTIN_LABELS.put("SEYAHAT",            "SEYAHAT");
        BUILTIN_LABELS.put("NILUFERKOY",         "NİLÜFERKÖY");
        BUILTIN_LABELS.put("ORTAK_GIDER",        "ORTAK GİDER");
        BUILTIN_LABELS.put("ALI_ALTIKARDESLER",  "ALİ ALTIKARDEŞLER");
        BUILTIN_LABELS.put("ATINC_ALTIKARDESLER","ATINÇ ALTIKARDEŞLER");
        BUILTIN_LABELS.put("KIVANC_ALTIKARDESLER","KIVANÇ ALTIKARDEŞLER");
        BUILTIN_LABELS.put("PERVIN_ALTIKARDESLER","PERVİN ALTIKARDEŞLER");
        BUILTIN_LABELS.put("VERGI_ODEMESI",      "VERGİ ÖDEMESİ");
        BUILTIN_LABELS.put("KIRA",               "KİRA");
        BUILTIN_LABELS.put("CEZA",               "CEZA");
        BUILTIN_LABELS.put("YAKIT",              "YAKIT");
        BUILTIN_LABELS.put("DIGER",              "DİĞER");
    }

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
            // Masraf türleri artık serbest string (custom kategoriler destekleniyor).
            jdbcTemplate.execute("ALTER TABLE tbl_expenses DROP CONSTRAINT IF EXISTS tbl_expenses_type_check");
            log.info("Expense type check constraint removed (custom categories enabled).");
        } catch (Exception e) {
            log.warn("Expense type constraint migration skipped: {}", e.getMessage());
        }
    }

    /** Built-in enum kategorilerini tbl_expense_categories'e (companyId=null) yaz, yoksa ekle */
    @PostConstruct
    public void seedBuiltinCategories() {
        try {
            int added = 0;
            for (Map.Entry<String, String> entry : BUILTIN_LABELS.entrySet()) {
                String code  = entry.getKey();
                String label = entry.getValue();

                // companyId IS NULL kontrolü için JdbcTemplate kullan (null parametresi güvenli)
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM tbl_expense_categories WHERE code = ? AND company_id IS NULL",
                        Integer.class, code);

                if (count == null || count == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO tbl_expense_categories (code, label, company_id) VALUES (?, ?, NULL)",
                            code, label);
                    added++;
                }
            }
            log.info("Built-in expense categories seeded: {} new record(s).", added);
        } catch (Exception e) {
            log.warn("Expense category seeding skipped: {}", e.getMessage());
        }
    }
}
