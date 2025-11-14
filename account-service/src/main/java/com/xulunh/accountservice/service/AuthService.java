package com.xulunh.accountservice.service;

import com.xulunh.accountservice.domain.UserAccount;
import com.xulunh.accountservice.repository.UserAccountRepository;
import com.xulunh.accountservice.security.JwtTokenService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final UserAccountRepository users;
    private final JwtTokenService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository users, JwtTokenService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    public String login(String email, String rawPassword) {
        UserAccount u = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!encoder.matches(rawPassword, u.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return jwt.create(u.getEmail(), Map.of("uid", u.getId()));
    }
}
