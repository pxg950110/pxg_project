package com.maidc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * MAIDC 合并单体应用：auth / task / label / audit / msg / data / model 的统一部署单元。
 * 各业务模块的启动类与 SecurityConfig 已删除（由本类与本包 config 统一提供）；
 * com.maidc.task / com.maidc.label 存在于 data/model 与 task/label 模块的两份副本，
 * 以 fat jar 类路径中靠前的独立模块版本为准（见 pom 依赖声明顺序）。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MaidcAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaidcAppApplication.class, args);
    }
}
