package com.xulunh.orderservice.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.math.BigDecimal;

@Getter
@Setter
@UserDefinedType("order_item")
public class OrderItem {
    private String itemId;
    private String upc;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;

}
