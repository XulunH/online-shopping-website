// payment-service/src/main/java/com/xulunh/paymentservice/config/SecurityConfig.java
package com.xulunh.paymentservice.config;

import com.xulunh.paymentservice.security.JwtAuthFilter;
import com.xulunh.paymentservice.security.JwtTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtTokenService tokenService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/accounts/register",
                                "/api/v1/auth/**",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            boolean hasAuth = req.getHeader(HttpHeaders.AUTHORIZATION) != null;
                            log.warn("401 Unauthorized: path={}, method={}, hasAuthHeader={}, msg={}",
                                    req.getRequestURI(), req.getMethod(), hasAuth, e.getMessage());
                            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            String principal = (auth != null ? auth.getName() : "null");
                            log.warn("403 AccessDenied: path={}, method={}, principal={}, msg={}",
                                    req.getRequestURI(), req.getMethod(), principal, e.getMessage());
                            res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                )
                .addFilterBefore(new JwtAuthFilter(tokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}