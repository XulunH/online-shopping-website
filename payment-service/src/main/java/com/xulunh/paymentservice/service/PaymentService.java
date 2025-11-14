package com.xulunh.paymentservice.service;

import com.xulunh.paymentservice.domain.Payment;
import com.xulunh.paymentservice.dto.PaymentRequest;
import com.xulunh.paymentservice.dto.PaymentResponse;
import com.xulunh.paymentservice.dto.RefundRequest;
import com.xulunh.paymentservice.events.PaymentEvents;
import com.xulunh.paymentservice.repository.PaymentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentEvents events;
    private final OrderGateway orders;

    public PaymentService(PaymentRepository payments, PaymentEvents events, OrderGateway orders) {
        this.payments = payments;
        this.events = events;
        this.orders = orders;
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrderId(), p.getStatus(), p.getAmount(),
                p.getAccountEmail(), p.getCreatedAt(), p.getUpdatedAt());
    }

    @Transactional
    public PaymentResponse submit(PaymentRequest req) {
        var existing = payments.findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) return toResponse(existing.get());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new IllegalStateException("Unauthenticated");
        String accountEmail = auth.getName();

        var order = orders.get(req.orderId());
        if (order == null) throw new IllegalStateException("Order not found");
        if (!"CREATED".equals(order.status)) throw new IllegalStateException("Order not in CREATED status");

        var p = new Payment();
        p.setId(UUID.randomUUID());
        p.setOrderId(req.orderId());
        p.setAccountEmail(accountEmail);
        p.setAmount(req.amount());
        p.setIdempotencyKey(req.idempotencyKey());
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(p.getCreatedAt());

        if (order.totalAmount == null || req.amount() == null || order.totalAmount.compareTo(req.amount()) != 0) {
            p.setStatus("FAILED");
            p = payments.save(p);
            return toResponse(p);
        }

        p.setStatus("SUCCESS");
        p = payments.save(p);
        events.publishSucceeded(p.getOrderId(), p.getId(), p.getAmount());
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID id) {
        return payments.findById(id).map(this::toResponse).orElseThrow();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrder(UUID orderId) {
        return payments.findByOrderId(orderId).map(this::toResponse).orElseThrow();
    }

    // Refund endpoint remains available if you still want manual refunds, but do NOT publish refunded here.
    @Transactional
    public PaymentResponse refund(UUID paymentId, RefundRequest req) {
        var p = payments.findById(paymentId).orElseThrow();
        if ("REFUNDED".equals(p.getStatus())) return toResponse(p);
        p.setStatus("REFUNDED");
        p.setUpdatedAt(Instant.now());
        p = payments.save(p);
        return toResponse(p);
    }
}