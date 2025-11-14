package com.xulunh.itemservice.repository;

import com.xulunh.itemservice.domain.Item;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ItemRepository extends MongoRepository<Item,String> {
    Optional<Item> findByUpc(String upc);
    boolean existsByUpc(String upc);
}
