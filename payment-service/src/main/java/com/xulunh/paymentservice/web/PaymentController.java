package com.xulunh.paymentservice.web;

import com.xulunh.paymentservice.dto.PaymentRequest;
import com.xulunh.paymentservice.dto.PaymentResponse;
import com.xulunh.paymentservice.dto.RefundRequest;
import com.xulunh.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
//    @PreAuthorize("isAuthenticated()")
    public PaymentResponse submit(@Valid @RequestBody PaymentRequest req) {
        return service.submit(req);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse refund(@PathVariable UUID id, @RequestBody(required = false) @Valid RefundRequest req) {

        return service.refund(id, req);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-order")
    public PaymentResponse getByOrder(@RequestParam UUID orderId) {
        return service.getByOrder(orderId);
    }
}
