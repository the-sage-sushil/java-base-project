package com.sushil.base_project.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sushil.base_project.core.security.CustomAccessDeniedHandler;
import com.sushil.base_project.core.security.CustomAuthenticationEntryPoint;
import com.sushil.base_project.core.security.JwtAuthenticationFilter;

import lombok.AllArgsConstructor;

@Configuration
@EnableMethodSecurity
@AllArgsConstructor
public class GlobalSecurityConfig {

    private final UserDetailsService userDetailService;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler CustomAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider is the standard provider that uses a
        // UserDetailsService and PasswordEncoder
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {

        return config.getAuthenticationManager();
    }

    @Order(0)
    @Bean
    public SecurityFilterChain swaggFilterChain(HttpSecurity http) throws Exception {

        http.securityMatcher("/swagger/open-api/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();

    }

    @Order(1)
    @Bean
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(CustomAccessDeniedHandler))

                .authenticationProvider(authenticationProvider())
                // Add the JWT filter right before the standard username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
