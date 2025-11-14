package com.xulunh.orderservice.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Table("orders")
public class Order {
    @PrimaryKeyColumn(name="id",type= PrimaryKeyType.PARTITIONED)
    private UUID id;

    @Column("account_email")
    private String accountEmail;

    @Column("status")
    private String status;

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("items")
    @CassandraType(type = CassandraType.Name.LIST, typeArguments = CassandraType.Name.UDT, userTypeName="order_item")
    private List<OrderItem> items;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;


}
