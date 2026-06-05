package com.sprint.analyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resumeAnalyzerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resume Analyzer API")
                        .description("REST APIs for resume upload, text extraction, and analysis")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Resume Analyzer Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://api.resume-analyzer.example.com").description("Production")
                ));
    }
}