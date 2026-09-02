package com.example.leagueticket.config;

import com.example.leagueticket.security.JwtAuthenticationFilter;
import com.example.leagueticket.security.RestAccessDeniedHandler;
import com.example.leagueticket.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("dev")
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/api/auth/login", "/api/auth/register", "/uploads/**").permitAll()
                        .requestMatchers("/api/user/**").hasRole("USER")
                        .requestMatchers("/api/club/**").hasRole("CLUB")
                        .requestMatchers(
                                "/api/admin/seasons",
                                "/api/admin/seasons/**",
                                "/api/admin/rounds/**",
                                "/api/admin/season-records/**",
                                "/api/admin/matches",
                                "/api/admin/matches/**",
                                "/api/admin/match-ticket-zones/**",
                                "/api/admin/match-seat-inventory/**",
                                "/api/admin/stadiums",
                                "/api/admin/stadiums/**",
                                "/api/admin/stadium-zones/**",
                                "/api/admin/stadium-seats/**",
                                "/api/admin/refunds",
                                "/api/admin/refunds/**",
                                "/api/admin/enrollments",
                                "/api/admin/enrollments/**",
                                "/api/admin/schedules",
                                "/api/admin/schedules/**",
                                "/api/admin/statistics/**")
                        .hasRole("EVENT_ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
