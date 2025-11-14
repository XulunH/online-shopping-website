package com.xulunh.orderservice.service;

import com.xulunh.orderservice.domain.Order;
import com.xulunh.orderservice.domain.OrderItem;
import com.xulunh.orderservice.dto.OrderCreateRequest;
import com.xulunh.orderservice.dto.OrderItemRequest;
import com.xulunh.orderservice.dto.OrderUpdateRequest;
import com.xulunh.orderservice.events.OrderEvents;
import com.xulunh.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private final OrderRepository repo = mock(OrderRepository.class);
    private final ItemGateway items = mock(ItemGateway.class);
    private final OrderEvents events = mock(OrderEvents.class);
    private final OrderService service = new OrderService(repo, items, events);

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

    private ItemGateway.ItemDto itemDto(String id, String upc, String name, String price) {
        var d = new ItemGateway.ItemDto();
        d.id = id;
        d.upc = upc;
        d.name = name;
        d.unitPrice = new BigDecimal(price);
        d.availableUnits = 100;
        return d;
    }

    @Test
    void create_success_buildsItems_andSaves() {
        setAuth("buyer@example.com");
        when(items.getByUpc("U1")).thenReturn(itemDto("i1", "U1", "N1", "3.50"));
        when(items.getByUpc("U2")).thenReturn(itemDto("i2", "U2", "N2", "1.25"));
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new OrderCreateRequest(List.of(
                new OrderItemRequest("U1", 2),
                new OrderItemRequest("U2", 4)
        ));
        var res = service.create(req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getAccountEmail()).isEqualTo("buyer@example.com");
        assertThat(captor.getValue().getItems()).hasSize(2);
        assertThat(res.totalAmount()).isEqualByComparingTo(new BigDecimal("3.50").multiply(BigDecimal.valueOf(2))
                .add(new BigDecimal("1.25").multiply(BigDecimal.valueOf(4))));
    }

    @Test
    void update_forbidden_whenDifferentAccount() {
        var id = UUID.randomUUID();
        var o = new Order();
        o.setId(id);
        o.setStatus("CREATED");
        o.setAccountEmail("owner@example.com");
        when(repo.findById(id)).thenReturn(Optional.of(o));

        setAuth("intruder@example.com");
        assertThatThrownBy(() -> service.update(id, new OrderUpdateRequest(List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void complete_throwsOnInsufficientInventory() {
        var id = UUID.randomUUID();
        var o = new Order();
        o.setId(id);
        o.setStatus("CREATED");
        var it = new OrderItem();
        it.setUpc("UPC1");
        it.setQuantity(5);
        o.setItems(List.of(it));

        when(repo.findById(id)).thenReturn(Optional.of(o));
        var dto = itemDto("i1", "UPC1", "N", "1.00");
        dto.availableUnits = 3;
        when(items.getByUpc("UPC1")).thenReturn(dto);

        assertThatThrownBy(() -> service.complete(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void cancel_onCompleted_restoresInventory_andPublishesEvent() {
        var id = UUID.randomUUID();
        var o = new Order();
        o.setId(id);
        o.setStatus("COMPLETED");
        var it = new OrderItem();
        it.setItemId("i1");
        it.setUpc("U1");
        it.setQuantity(2);
        o.setItems(List.of(it));
        o.setUpdatedAt(Instant.now());

        when(repo.findById(id)).thenReturn(Optional.of(o));
        when(repo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.cancel(id);
        assertThat(res.status()).isEqualTo("CANCELED");
        verify(items).adjustInventory("i1", 2);
        verify(events).publishCancelled(id);
    }
}


