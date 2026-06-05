package com.sprint.analyzer.config;

import com.sprint.analyzer.properties.DbProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.sprint.analyzer.repo")
@EnableConfigurationProperties(DbProperties.class)
public class DbConfig {

    private final DbProperties dbProperties;

    public DbConfig(DbProperties dbProperties) {
        this.dbProperties = dbProperties;
    }

    /**
     * Configures HikariCP DataSource for PostgreSQL connection pooling.
     */
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbProperties.getUrl());
        config.setUsername(dbProperties.getUsername());
        config.setPassword(dbProperties.getPassword());
        config.setDriverClassName(
            dbProperties.getDriverClassName() != null
                ? dbProperties.getDriverClassName()
                : "org.postgresql.Driver"
        );
        
        // Set pool sizes if provided
        if (dbProperties.getConnectionPoolMaxSize() != null) {
            config.setMaximumPoolSize(Integer.parseInt(dbProperties.getConnectionPoolMaxSize()));
        } else {
            config.setMaximumPoolSize(10); // Default
        }
        
        if (dbProperties.getConnectionPoolSize() != null) {
            config.setMinimumIdle(Integer.parseInt(dbProperties.getConnectionPoolSize()));
        } else {
            config.setMinimumIdle(2); // Default
        }

        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    /**
     * Configures EntityManagerFactory with Hibernate for PostgreSQL.
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.sprint.analyzer.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.dialect", 
            dbProperties.getHibernateDialect() != null
                ? dbProperties.getHibernateDialect()
                : "org.hibernate.dialect.PostgreSQLDialect"
        );
        properties.put("hibernate.hbm2ddl.auto",
            dbProperties.getHibernateHbm2ddlAuto() != null
                ? dbProperties.getHibernateHbm2ddlAuto()
                : "validate"
        );
        properties.put("hibernate.show_sql",
            dbProperties.getHibernateShowSql() != null
                ? dbProperties.getHibernateShowSql()
                : "false"
        );
        properties.put("hibernate.format_sql",
            dbProperties.getHibernateFormatSql() != null
                ? dbProperties.getHibernateFormatSql()
                : "true"
        );
        properties.put("hibernate.jdbc.batch_size", 50);
        properties.put("hibernate.order_inserts", true);
        properties.put("hibernate.order_updates", true);

        em.setJpaPropertyMap(properties);
        return em;
    }

    /**
     * Configures JPA TransactionManager.
     */
    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

}
