package com.maidc.data.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"com.maidc.common.jpa", "com.maidc.data.entity"})
@EnableJpaRepositories(basePackages = {"com.maidc.data.repository"})
public class JpaConfig {
}
