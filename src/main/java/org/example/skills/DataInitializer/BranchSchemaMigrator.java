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

/**
 * Hibernate ddl-auto:update çalışmadan ÖNCE şube kolonlarını ekler.
 * BeanFactoryPostProcessor olduğu için JPA EntityManagerFactory
 * kurulmadan önce tetiklenir — NOT NULL kısıtı sorunu olmaz.
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

        if (url == null || url.isBlank()) {
            log.warn("BranchSchemaMigrator: Datasource URL bulunamadı, atlanıyor.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(url, username, password)) {

            // 1. branch_type — nullable olarak ekle (NOT NULL YOK)
            conn.createStatement().execute("""
                ALTER TABLE company
                ADD COLUMN IF NOT EXISTS branch_type VARCHAR(50)
            """);

            // 2. Mevcut NULL satırları MERKEZ yap
            conn.createStatement().execute("""
                UPDATE company SET branch_type = 'MERKEZ'
                WHERE branch_type IS NULL
            """);

            // 3. parent_company_id
            conn.createStatement().execute("""
                ALTER TABLE company
                ADD COLUMN IF NOT EXISTS parent_company_id BIGINT
            """);

            log.info("BranchSchemaMigrator: Şube kolonları hazırlandı.");

        } catch (Exception e) {
            log.warn("BranchSchemaMigrator: {}", e.getMessage());
        }
    }
}
