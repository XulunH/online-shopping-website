package com.xulunh.accountservice.service;

import com.xulunh.accountservice.domain.UserAccount;
import com.xulunh.accountservice.repository.UserAccountRepository;
import com.xulunh.accountservice.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final JwtTokenService jwt = mock(JwtTokenService.class);
    private final AuthService service = new AuthService(users, jwt);

    @Test
    void login_success_returnsToken() {
        String hash = new BCryptPasswordEncoder().encode("pass");
        var u = UserAccount.builder()
                .id(7L)
                .email("a@b.com")
                .password(hash)
                .build();
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(jwt.create(eq("a@b.com"), anyMap())).thenReturn("token123");

        String token = service.login("a@b.com", "pass");
        assertThat(token).isEqualTo("token123");
    }

    @Test
    void login_throws_whenWrongPasswordOrUserNotFound() {
        when(users.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login("x@y.com", "any"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}


