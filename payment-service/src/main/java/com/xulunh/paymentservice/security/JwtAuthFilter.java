// payment-service/src/main/java/com/xulunh/paymentservice/security/JwtAuthFilter.java
package com.xulunh.paymentservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtTokenService tokenService;

    public JwtAuthFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String jwt = header.substring(7);
            try {
                Claims claims = tokenService.parse(jwt);
                String subject = claims.getSubject();
                var auth = new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("JWT OK: subject={}, path={}, method={}", subject, request.getRequestURI(), request.getMethod());
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                log.warn("JWT parse failed for path {}: {} (header present: {})",
                        request.getRequestURI(), e.getMessage(), true);
            }
        } else {
            log.debug("No Bearer token for path={}, method={}, authHeaderPresent={}",
                    request.getRequestURI(), request.getMethod(), header != null);
        }
        filterChain.doFilter(request, response);
    }
}
