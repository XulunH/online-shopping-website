package com.xulunh.accountservice.service;

import com.xulunh.accountservice.domain.Address;
import com.xulunh.accountservice.domain.UserAccount;
import com.xulunh.accountservice.dto.AccountCreateRequest;
import com.xulunh.accountservice.dto.AccountResponse;
import com.xulunh.accountservice.dto.AccountUpdateRequest;
import com.xulunh.accountservice.dto.AddressDto;
import com.xulunh.accountservice.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder=  new BCryptPasswordEncoder();

    public AccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest req) {
        if (userAccountRepository.existsByEmail(req.email())){
            throw new IllegalStateException("Email already exists");
        }
        if(userAccountRepository.existsByUsername(req.username())){
            throw new IllegalStateException("Username already exists");
        }
        UserAccount userAccount = UserAccount.builder()
                .email(req.email())
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .shipAddress(toAddress(req.shippingAddress()))
                .billingAddress(toAddress(req.billingAddress()))
                .build();
        UserAccount createdUserAccount = userAccountRepository.save(userAccount); //returns the actual object in db managed by orm
        return toResponse(createdUserAccount);
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest req) {
        UserAccount u = userAccountRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Account not found"));

        // if username changes, make sure it's unique
        if (!u.getUsername().equals(req.username()) && userAccountRepository.existsByUsername(req.username())) {
            throw new IllegalStateException("Username already in use");
        }

        u.setUsername(req.username());
        u.setShipAddress(toAddress(req.shippingAddress()));
        u.setBillingAddress(toAddress(req.billingAddress()));

        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(Long id) {
        UserAccount u = userAccountRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByEmail(String email) {
        UserAccount u = userAccountRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByUsername(String username) {
        UserAccount u = userAccountRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return toResponse(u);
    }

    private AccountResponse toResponse(UserAccount u) {
        return new AccountResponse(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                toDto(u.getShipAddress()),
                toDto(u.getBillingAddress())
        );
    }

    private Address toAddress(AddressDto d) {
        if (d == null) return null;
        Address a = new Address();
        a.setLine1(d.line1());
        a.setLine2(d.line2());
        a.setCity(d.city());
        a.setState(d.state());
        a.setZip(d.zip());
        a.setCountry(d.country());
        return a;
    }

    private AddressDto toDto(Address a) {
        if (a == null) return null;
        return new AddressDto(a.getLine1(), a.getLine2(), a.getCity(), a.getState(), a.getZip(), a.getCountry());
    }
}
