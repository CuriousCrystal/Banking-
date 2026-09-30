package com.amc.bfsi.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

/**
 * Production start-up check: names every missing setting at once ("DB_URL, JWT_SECRET") instead of
 * failing later on the first cryptic symptom ("URL must start with 'jdbc'"). Spring's property binding
 * leaves an unresolved ${DB_URL} as literal text, which is why the check has to look for it.
 */
@Configuration
@Profile("prod")
public class ProdSettingsCheck {

    /** property -> the environment variable an operator sets for it (see application-prod.properties) */
    private static final Map<String, String> REQUIRED = Map.of(
            "spring.datasource.url", "DB_URL",
            "spring.datasource.username", "DB_USERNAME",
            "spring.datasource.password", "DB_PASSWORD",
            "app.jwt.secret", "JWT_SECRET");

    @Bean
    static BeanFactoryPostProcessor requireProductionSettings(Environment env) {
        return beanFactory -> {
            List<String> missing = REQUIRED.entrySet().stream()
                    .filter(entry -> !isSet(env, entry.getKey()))
                    .map(Map.Entry::getValue)
                    .sorted()
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "Missing required environment variables for the prod profile: " + String.join(", ", missing));
            }
        };
    }

    private static boolean isSet(Environment env, String property) {
        try {
            String value = env.getProperty(property);
            return value != null && !value.contains("${");
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            return false;
        }
    }
}
