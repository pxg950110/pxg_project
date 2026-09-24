package com.maidc.app.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * MapStruct 生成实现类的显式注册（按类名注册，构建期不引用 Impl 类）。
 * 背景：com.maidc.task / com.maidc.label 包在 task/label 模块与 data/model 模块中各有一份
 * 分叉副本（fat jar 类路径遮蔽），编译期/扫描期对重复 FQN 的解析不稳定；
 * 按类名注册运行时由类加载器取首个命中（依赖顺序在前 = 独立模块版本）。
 */
@Configuration
public class MapperBeanConfig implements BeanDefinitionRegistryPostProcessor {

    private static final String[] MAPPER_IMPLS = {
            "com.maidc.audit.mapper.AuditMapperImpl",
            "com.maidc.data.mapper.DataMapperImpl",
            "com.maidc.label.mapper.LabelMapperImpl",
            "com.maidc.model.mapper.ModelMapperImpl",
            "com.maidc.msg.mapper.MsgMapperImpl",
            "com.maidc.task.mapper.PersonalTaskMapperImpl",
            "com.maidc.task.mapper.TaskMapperImpl",
    };

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String impl : MAPPER_IMPLS) {
            String simple = impl.substring(impl.lastIndexOf('.') + 1);
            // 组件扫描按驼峰首字母小写命名；扫描已注册（正常路径）则跳过，仅作缺位兜底
            String beanName = Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
            if (!registry.containsBeanDefinition(beanName)) {
                registry.registerBeanDefinition(beanName,
                        BeanDefinitionBuilder.genericBeanDefinition(impl).getBeanDefinition());
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 无需处理
    }
}
