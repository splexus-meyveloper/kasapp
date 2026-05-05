package org.example.skills.DataInitializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skills.enums.PosTerminal;
import org.example.skills.enums.PosType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PosSchemaMigrator {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migratePosConstraints() {
        try {
            synchronizeEnumConstraint(
                    "tbl_pos_log",
                    "tbl_pos_log_pos_type_check",
                    "pos_type",
                    Arrays.stream(PosType.values()).map(Enum::name).toArray(String[]::new)
            );
            synchronizeEnumConstraint(
                    "tbl_pos_log",
                    "tbl_pos_log_terminal_check",
                    "terminal",
                    Arrays.stream(PosTerminal.values()).map(Enum::name).toArray(String[]::new)
            );
            log.info("POS check constraints synchronized.");
        } catch (Exception e) {
            log.warn("POS constraint migration skipped: {}", e.getMessage());
        }
    }

    private void synchronizeEnumConstraint(String tableName,
                                           String constraintName,
                                           String columnName,
                                           String[] values) {
        String allowed = Arrays.stream(values)
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(","));

        jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + constraintName);
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD CONSTRAINT " + constraintName
                + " CHECK (" + columnName + " IN (" + allowed + "))");
    }
}
