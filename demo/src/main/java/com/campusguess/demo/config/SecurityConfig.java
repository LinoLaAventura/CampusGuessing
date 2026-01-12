package com.campusguess.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// 标记为配置类，Spring会扫描并加载其中的Bean
@Configuration
public class SecurityConfig {

    // 声明PasswordEncoder Bean，Spring容器会管理这个实例
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用BCrypt加密算法（不可逆、带盐值，安全）
        return new BCryptPasswordEncoder();
    }

    // 暂时禁用了
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())); // 允许同源iframe，WebSocket需要
        return http.build();
    }
}