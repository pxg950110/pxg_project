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

    // Embulk 抽取任务的源/目标库连接（生成 YAML 用；接入 DataSource 实体前的占位默认值）
    private String sourceHost = "localhost";
    private int sourcePort = 5432;
    private String sourceDatabase = "source_db";
    private String sourceUser = "source_user";
    private String sourcePassword = "source_pass";
    private String targetHost = "localhost";
    private int targetPort = 5432;
    private String targetDatabase = "target_db";
    private String targetUser = "target_user";
    private String targetPassword = "target_pass";
}
