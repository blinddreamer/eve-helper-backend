package com.example.pandatribe.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/authorized/**", "/api/v1/me").authenticated()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/**"))
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/api/v1/callback", true) // Redirect to your callback handler
                )
                .logout(logout -> logout.logoutUrl("/api/v1/logout").logoutSuccessUrl("/"));

        return http.build();
    }

}