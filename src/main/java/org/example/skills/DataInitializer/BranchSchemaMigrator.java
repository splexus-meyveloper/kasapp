package org.example.skills.DataInitializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

/**
 * Hibernate ddl-auto:update çalışmadan ÖNCE şube kolonlarını ekler.
 * Mevcut branch_type değerlerine DOKUNMAZ — sadece NULL olanları doldurur.
 */
@Component
@Slf4j
public class BranchSchemaMigrator implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment env;

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {

        String url      = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        if (url == null || url.isBlank()) return;

        try (Connection conn = DriverManager.getConnection(url, username, password)) {

            // 1. Kolonları nullable olarak ekle — varsa atla
            conn.createStatement().execute(
                "ALTER TABLE company ADD COLUMN IF NOT EXISTS branch_type VARCHAR(50)");
            conn.createStatement().execute(
                "ALTER TABLE company ADD COLUMN IF NOT EXISTS parent_company_id BIGINT");

            // 2. Sadece branch_type = NULL olanları doldur — mevcut değerlere DOKUNMA
            // En küçük ID = MERKEZ, diğerleri = SUBE
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM company WHERE branch_type IS NULL");
            rs.next();
            int nullCount = rs.getInt(1);

            if (nullCount > 0) {
                long merkezId = -1;
                ResultSet rs2 = conn.createStatement().executeQuery("SELECT MIN(id) FROM company");
                if (rs2.next()) merkezId = rs2.getLong(1);

                if (merkezId > 0) {
                    conn.createStatement().execute(
                        "UPDATE company SET branch_type = 'MERKEZ', parent_company_id = NULL " +
                        "WHERE id = " + merkezId + " AND branch_type IS NULL");
                    conn.createStatement().execute(
                        "UPDATE company SET branch_type = 'SUBE', parent_company_id = " + merkezId +
                        " WHERE id != " + merkezId + " AND branch_type IS NULL");
                    log.info("BranchSchemaMigrator: {} kayıt güncellendi.", nullCount);
                }
            } else {
                log.info("BranchSchemaMigrator: branch_type zaten tanımlı, dokunulmadı.");
            }

        } catch (Exception e) {
            log.warn("BranchSchemaMigrator: {}", e.getMessage());
        }
    }
}
