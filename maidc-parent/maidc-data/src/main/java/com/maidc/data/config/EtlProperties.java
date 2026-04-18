package com.maidc.data.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "maidc.etl")
public class EtlProperties {

    private String csvBaseDir;
    private int parallel = 3;
    private String dbUrl;
    private String dbSchema = "ods";
    private String dbUser;
    private String dbPassword;
    private String ddlPath;     // DDL SQL files directory, e.g. E:/pxg_project/docker/init-db
}
