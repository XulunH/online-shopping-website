package com.xulunh.paymentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "account_email", nullable = false)
    private String accountEmail;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;


    @Column(name = "status", nullable = false, length = 20)
    private String status; // INITIATED, CAPTURED, FAILED, REFUNDED

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
