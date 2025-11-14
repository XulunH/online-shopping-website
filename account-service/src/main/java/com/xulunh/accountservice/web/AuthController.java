package com.xulunh.accountservice.web;

import com.xulunh.accountservice.dto.LoginRequest;
import com.xulunh.accountservice.dto.LoginResponse;
import com.xulunh.accountservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        String token= auth.login(loginRequest.email(),  loginRequest.password());
        return new LoginResponse(token);
    }
}
