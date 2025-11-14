package com.xulunh.orderservice.service;

import com.xulunh.orderservice.domain.Order;
import com.xulunh.orderservice.domain.OrderItem;
import com.xulunh.orderservice.dto.OrderCreateRequest;
import com.xulunh.orderservice.dto.OrderResponse;
import com.xulunh.orderservice.events.OrderEvents;
import com.xulunh.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
     private final OrderRepository orderRepository;
     private final ItemGateway itemGateway;
    private final OrderEvents orderEvents;
    public OrderService(OrderRepository orders, ItemGateway items, OrderEvents events) {
        this.orderRepository = orders;
        this.itemGateway = items;
        this.orderEvents = events;
    }
     private OrderResponse toResponse(Order order){
         var itemDtos= order.getItems()==null? List.<OrderResponse.Item>of()
                 :order.getItems().stream()
                 .map(i->new OrderResponse.Item(i.getItemId(),i.getUpc(),i.getName(),i.getUnitPrice(),i.getQuantity()))
                 .toList();
         return new OrderResponse(order.getId(),order.getAccountEmail(), order.getStatus(), order.getTotalAmount(),
                 itemDtos, order.getCreatedAt(), order.getUpdatedAt());
     }

     private record PreparedItem(String itemId, String upc, String name, BigDecimal unitPrice, int quantity) {}

    public List<OrderResponse> getAll() {
         return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getByAccountEmail(String accountEmail) {
         return orderRepository.findByAccountEmail(accountEmail).stream().map(this::toResponse).toList();
    }
     @Transactional(readOnly = true)
     public OrderResponse get(UUID id) {
           return orderRepository.findById(id).map(this::toResponse).orElseThrow();
     }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public OrderResponse create(OrderCreateRequest req){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        String accountEmail = auth.getName();
        var order = new Order();
        order.setId(UUID.randomUUID());
        order.setAccountEmail(accountEmail);
        order.setStatus("CREATED");
        order.setUpdatedAt(Instant.now());
        order.setCreatedAt(order.getUpdatedAt());

        var orderItems = new ArrayList<OrderItem>();
        BigDecimal total = BigDecimal.ZERO;

        for (var it : req.items()) {
            var item = itemGateway.getByUpc(it.upc());
            if (item == null) {
                throw new IllegalStateException("Item " + it.upc() + " not found");
            }
            var oi = new OrderItem();
            oi.setItemId(item.id);
            oi.setUpc(item.upc);
            oi.setName(item.name);
            oi.setUnitPrice(item.unitPrice);
            oi.setQuantity(it.quantity());
            orderItems.add(oi);

            total = total.add(item.unitPrice.multiply(BigDecimal.valueOf(it.quantity())));
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);

        var saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public OrderResponse update(UUID id, com.xulunh.orderservice.dto.OrderUpdateRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        var o = orderRepository.findById(id).orElseThrow();
        if (!"CREATED".equals(o.getStatus())) {
            throw new IllegalStateException("Only orders in CREATED status can be updated");
        }
        if (!auth.getName().equals(o.getAccountEmail())) {
            throw new IllegalStateException("Forbidden");
        }

        var updatedItems = new ArrayList<OrderItem>();
        BigDecimal total = BigDecimal.ZERO;

        for (var it : req.items()) {
            var item = itemGateway.getByUpc(it.upc());
            if (item == null) {
                throw new IllegalStateException("Item " + it.upc() + " not found");
            }
            var oi = new OrderItem();
            oi.setItemId(item.id);
            oi.setUpc(item.upc);
            oi.setName(item.name);
            oi.setUnitPrice(item.unitPrice);
            oi.setQuantity(it.quantity());
            updatedItems.add(oi);

            total = total.add(item.unitPrice.multiply(BigDecimal.valueOf(it.quantity())));
        }

        o.setItems(updatedItems);
        o.setTotalAmount(total);
        o.setUpdatedAt(Instant.now());
        o = orderRepository.save(o);
        return toResponse(o);
    }
    @Transactional
    public OrderResponse complete(UUID id) {
        var o = orderRepository.findById(id).orElseThrow();
        if (!"CREATED".equals(o.getStatus())) {
            throw new IllegalStateException("Order not in CREATED status");
        }

        // Pre-check availability
        for (var it : o.getItems()) {
            var item = itemGateway.getByUpc(it.getUpc());
            int available = item == null || item.availableUnits == null ? 0 : item.availableUnits;
            if (available < it.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for UPC " + it.getUpc());
            }
        }

        // Deduct inventory with best-effort rollback
        var deducted = new ArrayList<OrderItem>();
        try {
            for (var it : o.getItems()) {
                itemGateway.adjustInventory(it.getItemId(), -it.getQuantity());
                deducted.add(it);
            }
        } catch (RestClientException ex) {
            for (var r : deducted) {
                try { itemGateway.adjustInventory(r.getItemId(), r.getQuantity()); } catch (Exception ignore) {}
            }
            throw new IllegalStateException("Failed to deduct inventory for completion");
        }

        o.setStatus("COMPLETED");
        o.setUpdatedAt(Instant.now());
        o = orderRepository.save(o);
        return toResponse(o);
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        var o = orderRepository.findById(id).orElseThrow();
        if ("CANCELED".equals(o.getStatus())) {
            return toResponse(o);
        }

        if ("COMPLETED".equals(o.getStatus())) {
            for (var it : o.getItems()) {
                try {
                    itemGateway.adjustInventory(it.getItemId(), it.getQuantity());
                } catch (Exception ignore) {}
            }
        }

        o.setStatus("CANCELED");
        o.setUpdatedAt(Instant.now());
        o = orderRepository.save(o);
        try { orderEvents.publishCancelled(o.getId()); } catch (Exception ignored) {}
        return toResponse(o);
    }



}
