package com.yowyob.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie le chargement du contexte Spring (profil test).
 */
@SpringBootTest
@ActiveProfiles("test")
class YowyobPaymentApplicationTests {

    @Test
    void contextLoads() {
    }
}
