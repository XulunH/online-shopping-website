package com.xulunh.paymentservice.events;

import com.xulunh.paymentservice.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventsListener {
    private final PaymentRepository payments;

    public OrderEventsListener(PaymentRepository payments) { this.payments = payments; }

    @KafkaListener(topics = "order.events", groupId = "payment-service")
    public void onOrderEvent(ConsumerRecord<String, OrderEvent> record) {
        var evt = record.value();
        if (evt == null || !"OrderCancelled".equals(evt.type())) return;

        payments.findByOrderId(evt.orderId()).ifPresent(p -> {
            if (!"REFUNDED".equals(p.getStatus())) {
                p.setStatus("REFUNDED");
                p.setUpdatedAt(Instant.now());
                payments.save(p);
            }
        });
    }

    public record OrderEvent(String type, UUID orderId) {}
}
