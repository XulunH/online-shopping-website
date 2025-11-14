package com.xulunh.orderservice.web;

import com.xulunh.orderservice.dto.OrderCreateRequest;
import com.xulunh.orderservice.dto.OrderResponse;
import com.xulunh.orderservice.dto.OrderUpdateRequest;
import com.xulunh.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest req) {
          return orderService.create(req);
    }
    @GetMapping
    public List<OrderResponse> getOrders() {
        return orderService.getAll();
    }
    @GetMapping("/by-accountEmail")
    public List<OrderResponse> getOrdersByAccountEmail(@RequestParam("accountEmail") String accountEmail) {
        return orderService.getByAccountEmail(accountEmail);
    }
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.get(id);
    }
    @PostMapping("/{id}/complete")
    public OrderResponse complete(@PathVariable UUID id) {
        return orderService.complete(id);
    }
    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable UUID id, @Valid @RequestBody OrderUpdateRequest req) {
        return orderService.update(id, req);
    }
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        return orderService.cancel(id);
    }
}
