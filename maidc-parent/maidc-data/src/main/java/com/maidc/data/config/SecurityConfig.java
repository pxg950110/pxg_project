package com.maidc.data.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.Serializable;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = false)  // dev 环境禁用方法安全
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Profile("dev")
    @Configuration
    static class DevSecurityConfig {

        @Autowired
        private CorsConfigurationSource corsConfigurationSource;

        @Bean
        public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/**").permitAll()
                            .anyRequest().permitAll());
            return http.build();
        }
    }

    @Profile("!dev")
    @Configuration
    static class ProdSecurityConfig {

        @Autowired
        private CorsConfigurationSource corsConfigurationSource;

        @Bean
        public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/**").permitAll()
                            .anyRequest().authenticated());
            return http.build();
        }
    }
}

/**
 * Dev 环境下跳过 @PreAuthorize 权限检查
 * 通过返回 always true 的 PermissionEvaluator 实现
 */
@Profile("dev")
@Configuration
class DevPermissionConfig {

    @Bean
    public org.springframework.security.access.PermissionEvaluator permissionEvaluator() {
        return new org.springframework.security.access.PermissionEvaluator() {
            @Override
            public boolean hasPermission(org.springframework.security.core.Authentication authentication,
                                          Object targetDomainObject, Object permission) {
                return true;
            }

            @Override
            public boolean hasPermission(org.springframework.security.core.Authentication authentication,
                                          Serializable targetId, String targetType, Object permission) {
                return true;
            }
        };
    }
}
