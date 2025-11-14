package com.xulunh.itemservice.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Document(collection="items")
public class Item {
    @Id
    private String id;
    private String name;
    private String upc;
    private BigDecimal unitPrice;
    private List<String> pictureUrls;
    private Integer availableUnits;
}
