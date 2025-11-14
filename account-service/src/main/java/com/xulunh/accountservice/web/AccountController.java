package com.xulunh.accountservice.web;

import com.xulunh.accountservice.dto.AccountCreateRequest;
import com.xulunh.accountservice.dto.AccountResponse;
import com.xulunh.accountservice.dto.AccountUpdateRequest;
import com.xulunh.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest req) {
        return accounts.create(req);
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id) {
        return accounts.getById(id);
    }

    @GetMapping("/by-email")
    public AccountResponse getByEmail(@RequestParam String email) {
        return accounts.getByEmail(email);
    }

    @GetMapping("/by-username")
    public AccountResponse getByUsername(@RequestParam String username) {
        return accounts.getByUsername(username);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest req) {
        return accounts.update(id, req);
    }
}
