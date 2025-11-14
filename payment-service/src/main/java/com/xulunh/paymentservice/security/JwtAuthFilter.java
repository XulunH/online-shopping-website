package com.xulunh.paymentservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenService tokenService;
    public JwtAuthFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header= request.getHeader(HttpHeaders.AUTHORIZATION);
        if ( header != null && header.startsWith("Bearer ") ) {
            String jwt= header.substring(7);
            try{
                Claims claims= tokenService.parse(jwt);
                String subject= claims.getSubject(); //user's email
                var auth= new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            catch (Exception e){
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request,response);
    }
}
