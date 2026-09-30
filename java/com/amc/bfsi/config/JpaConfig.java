package com.amc.bfsi.config;

import com.amc.bfsi.repository.TenantSafeRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Every repository extends TenantSafeRepository, so by-id lookups respect the tenant filter too. */
@Configuration
@EnableJpaRepositories(basePackages = "com.amc.bfsi.repository", repositoryBaseClass = TenantSafeRepository.class)
public class JpaConfig {
}
