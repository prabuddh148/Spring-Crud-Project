package com.example.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

                // Disable CSRF
                .csrf(csrf -> csrf.disable())

                // JWT based authentication
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth

                        // Frontend files served from resources/static.
                        // These need AntPathRequestMatcher: Spring cannot tell
                        // whether a root-level path is an MVC route or a static file.
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/"),
                                AntPathRequestMatcher.antMatcher("/index.html"),
                                AntPathRequestMatcher.antMatcher("/app.js"),
                                AntPathRequestMatcher.antMatcher("/styles.css"),
                                AntPathRequestMatcher.antMatcher("/favicon.ico"),
                                // without this, any thrown exception forwards to
                                // /error, gets blocked, and surfaces as an empty 403
                                AntPathRequestMatcher.antMatcher("/error"))
                        .permitAll()

                        // Public APIs
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/auth/register"),
                                AntPathRequestMatcher.antMatcher("/auth/login"),
                                AntPathRequestMatcher.antMatcher("/auth/refresh-token"))
                        .permitAll()

                        // Only ADMIN
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher(
                                        HttpMethod.DELETE,
                                        "/users/**"))
                        .hasRole("ADMIN")

                        // USER + ADMIN
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/users/**"))
                        .hasAnyRole(
                                "USER",
                                "ADMIN")

                        // Everything else requires login
                        .anyRequest().authenticated())

                // Run our JWT filter before Spring's authentication filter
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}