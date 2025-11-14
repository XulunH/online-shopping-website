package com.xulunh.orderservice.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ItemGateway {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    public ItemGateway(RestTemplate restTemplate, @Value("${item.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ItemDto getByUpc(String upc){
        String url= UriComponentsBuilder.fromUriString(baseUrl+"/api/v1/items/by-upc").queryParam("upc", upc).toUriString();
        return restTemplate.getForObject(url,ItemDto.class);
    }

    public ItemDto adjustInventory(String id, int delta){
        String url= UriComponentsBuilder.fromUriString(baseUrl+"/api/v1/items/{id}/inventory").queryParam("delta", delta).build(id).toString();
        RequestEntity<Void> requestEntity = RequestEntity.method(HttpMethod.PATCH,url).build();
        ResponseEntity<ItemDto> res= restTemplate.exchange(requestEntity,ItemDto.class);
        return res.getBody();
    }

    public static class ItemDto{
        public String id;
        public String upc;
        public String name;
        public BigDecimal unitPrice;
        public List<String> pictureUrls;
        public Integer availableUnits;
    }
}
