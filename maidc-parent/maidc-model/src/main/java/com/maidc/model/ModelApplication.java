package com.maidc.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.maidc.model", "com.maidc.label", "com.maidc.common"})
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = {"com.maidc.model.repository", "com.maidc.label.repository"})
@EntityScan(basePackages = {"com.maidc.model.entity", "com.maidc.label.entity"})
public class ModelApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModelApplication.class, args);
    }
}
