package com.xulunh.accountservice.service;

import com.xulunh.accountservice.domain.UserAccount;
import com.xulunh.accountservice.dto.AccountCreateRequest;
import com.xulunh.accountservice.dto.AccountUpdateRequest;
import com.xulunh.accountservice.dto.AddressDto;
import com.xulunh.accountservice.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

class AccountServiceTest {

    private final UserAccountRepository repo = mock(UserAccountRepository.class);
    private final AccountService service = new AccountService(repo);

    @Test
    void create_success_hashesPassword_andSaves() {
        when(repo.existsByEmail("a@b.com")).thenReturn(false);
        when(repo.existsByUsername("alice")).thenReturn(false);
        when(repo.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AccountCreateRequest req = new AccountCreateRequest(
                "a@b.com", "alice", "PlainPass",
                new AddressDto("l1", null, "NYC", "NY", "10001", "US"),
                null
        );

        var resp = service.create(req);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.email()).isEqualTo("a@b.com");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isNotEqualTo("PlainPass");
        assertThat(captor.getValue().getPassword()).startsWith("$2"); // bcrypt hash
    }

    @Test
    void create_throwsOnDuplicateEmail() {
        when(repo.existsByEmail("a@b.com")).thenReturn(true);

        var req = new AccountCreateRequest("a@b.com", "alice", "x", null, null);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void create_throwsOnDuplicateUsername() {
        when(repo.existsByEmail("a@b.com")).thenReturn(false);
        when(repo.existsByUsername("alice")).thenReturn(true);

        var req = new AccountCreateRequest("a@b.com", "alice", "x", null, null);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void update_success_changesUsernameAndAddresses() {
        UserAccount u = UserAccount.builder()
                .id(5L).email("a@b.com").username("old").password("hash").build();

        when(repo.findById(5L)).thenReturn(Optional.of(u));
        when(repo.existsByUsername("newname")).thenReturn(false);

        var req = new AccountUpdateRequest("newname", new AddressDto("l1", null, "NYC", "NY", "10001", "US"), null);
        var resp = service.update(5L, req);

        assertThat(resp.username()).isEqualTo("newname");
        assertThat(resp.shippingAddress().line1()).isEqualTo("l1");
    }

    @Test
    void update_throwsIfNotFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(9L, new AccountUpdateRequest("x", null, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_throwsIfUsernameTaken() {
        UserAccount u = UserAccount.builder()
                .id(1L).email("a@b.com").username("old").password("hash").build();

        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new AccountUpdateRequest("taken", null, null)))
                .isInstanceOf(IllegalStateException.class);
    }
}