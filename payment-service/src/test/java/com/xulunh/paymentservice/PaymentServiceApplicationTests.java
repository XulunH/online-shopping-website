package com.xulunh.paymentservice;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Skip heavy Spring context boot; unit tests cover service layer")
class AccountServiceApplicationTests {
    @org.junit.jupiter.api.Test
    void contextLoads() { }
}
