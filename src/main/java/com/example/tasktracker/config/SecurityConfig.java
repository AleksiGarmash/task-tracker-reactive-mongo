package com.example.tasktracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Set;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @Primary
    public ReactiveUserDetailsService userDetailsService(PasswordEncoder encoder) {

        String pass = encoder.encode("password");

        UserDetails manager = User.withUsername("manager")
                .password(pass)
                .roles("MANAGER")
                .build();

        UserDetails user = User.withUsername("user")
                .password(pass)
                .roles("USER")
                .build();

        return new MapReactiveUserDetailsService(manager, user);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Users: USER + MANAGER
                        .pathMatchers("/users/**").hasAnyRole("USER", "MANAGER")

                        // Tasks: READ для всех + WRITE только MANAGER
                        .pathMatchers(HttpMethod.GET,"/task", "/task/{id}").hasAnyRole("USER", "MANAGER")
                        .pathMatchers(HttpMethod.POST, "/task").hasRole("MANAGER")
                        .pathMatchers(HttpMethod.PUT, "/task/{id}").hasRole("MANAGER")
                        .pathMatchers(HttpMethod.PATCH, "/task/{id}/observers/{observerId}").hasRole("MANAGER")
                        .pathMatchers(HttpMethod.DELETE, "/task/{id}").hasRole("MANAGER")
                        .anyExchange().permitAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
