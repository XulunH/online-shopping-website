package com.xulunh.paymentservice.service;

import com.xulunh.paymentservice.domain.Payment;
import com.xulunh.paymentservice.dto.PaymentRequest;
import com.xulunh.paymentservice.dto.PaymentResponse;
import com.xulunh.paymentservice.dto.RefundRequest;
import com.xulunh.paymentservice.events.PaymentEvents;
import com.xulunh.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private final PaymentRepository repo = mock(PaymentRepository.class);
    private final PaymentEvents events = mock(PaymentEvents.class);
    private final OrderGateway orders = mock(OrderGateway.class);
    private final PaymentService service = new PaymentService(repo, events, orders);

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void submit_returnsExisting_whenOrderAlreadyHasPayment() {
        var payment = new Payment();
        var orderId = UUID.randomUUID();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setStatus("SUCCESS");
        when(repo.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        setAuth("user@example.com");
        var res = service.submit(new PaymentRequest(orderId, new BigDecimal("10.00")));
        assertThat(res.id()).isEqualTo(payment.getId());
        verify(repo, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void submit_throws_whenUnauthenticated() {
        var orderId = UUID.randomUUID();
        assertThatThrownBy(() -> service.submit(new PaymentRequest(orderId, new BigDecimal("5.00"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submit_fails_whenAmountMismatch() {
        var orderId = UUID.randomUUID();
        var dto = new OrderGateway.OrderDto();
        dto.id = orderId;
        dto.status = "CREATED";
        dto.totalAmount = new BigDecimal("10.00");
        when(orders.get(orderId)).thenReturn(dto);
        when(repo.findByOrderId(orderId)).thenReturn(Optional.empty());

        when(repo.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(p.getCreatedAt());
            return p;
        });

        setAuth("user@example.com");
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        PaymentResponse resp = service.submit(new PaymentRequest(orderId, new BigDecimal("9.99")));
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(resp.status()).isEqualTo("FAILED");
        verifyNoInteractions(events);
    }

    @Test
    void submit_success_publishesEvent() {
        var orderId = UUID.randomUUID();
        var dto = new OrderGateway.OrderDto();
        dto.id = orderId;
        dto.status = "CREATED";
        dto.totalAmount = new BigDecimal("15.50");
        when(orders.get(orderId)).thenReturn(dto);
        when(repo.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(repo.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(p.getCreatedAt());
            return p;
        });

        setAuth("user@example.com");
        var res = service.submit(new PaymentRequest(orderId, new BigDecimal("15.50")));
        assertThat(res.status()).isEqualTo("SUCCESS");
        verify(events).publishSucceeded(eq(orderId), any(UUID.class), eq(new BigDecimal("15.50")));
    }

    @Test
    void refund_isIdempotent_setsRefundedOnce() {
        var pid = UUID.randomUUID();
        var p = new Payment();
        p.setId(pid);
        p.setOrderId(UUID.randomUUID());
        p.setStatus("SUCCESS");
        when(repo.findById(pid)).thenReturn(Optional.of(p));
        when(repo.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var r1 = service.refund(pid, new RefundRequest(new BigDecimal("1.00")));
        assertThat(r1.status()).isEqualTo("REFUNDED");

        var r2 = service.refund(pid, new RefundRequest(new BigDecimal("1.00")));
        assertThat(r2.status()).isEqualTo("REFUNDED");
        verify(repo, times(1)).save(any(Payment.class));
    }
}


