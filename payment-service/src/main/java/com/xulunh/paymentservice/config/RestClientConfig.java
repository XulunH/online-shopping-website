package com.xulunh.paymentservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors((request, body, execution) -> {
                    var attrs = RequestContextHolder.getRequestAttributes();
                    if (attrs instanceof ServletRequestAttributes sra) {
                        String auth = sra.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                        if (auth != null && !auth.isBlank()) {
                            request.getHeaders().add(HttpHeaders.AUTHORIZATION, auth);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}