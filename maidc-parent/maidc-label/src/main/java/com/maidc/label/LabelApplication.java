package com.maidc.label;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.maidc.label", "com.maidc.common"})
@EnableDiscoveryClient
public class LabelApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelApplication.class, args);
    }
}
