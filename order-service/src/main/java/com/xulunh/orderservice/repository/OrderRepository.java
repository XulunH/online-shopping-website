package com.xulunh.orderservice.repository;

import com.xulunh.orderservice.domain.Order;

import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends CassandraRepository<Order, UUID> {
    @AllowFiltering
    List<Order> findByAccountEmail(String accountEmail);
}
