package com.xulunh.orderservice.repository;

import com.xulunh.orderservice.domain.Order;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends CassandraRepository<Order, UUID> {
    List<Order> findByAccountEmail(String accountEmail);

    // Add this custom query method
    @Query("SELECT * FROM orders WHERE id = :id")
    Optional<Order> findOrderById(@Param("id") UUID id);
}
