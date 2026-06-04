package com.sprint.analyzer.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "spring.datasource")
@Configuration
@Data
public class DbProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String hibernateDialect;
    private String hibernateHbm2ddlAuto;
    private String hibernateShowSql;
    private String hibernateFormatSql;
    private String connectionPoolSize;
    private String connectionPoolMaxSize;

}
