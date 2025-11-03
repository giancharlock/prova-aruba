package com.experis.receiver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Aggiungi la proprietà per disabilitare springdoc solo in questo test
@SpringBootTest(properties = "springdoc.api-docs.enabled=false")
class ReceiverApplicationTests {

    @Test
    void contextLoads() {
    }

}