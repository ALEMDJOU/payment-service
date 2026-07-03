package com.yowyob.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API Yowyob Payment (wallets, transactions, Stripe).
 */
@SpringBootApplication
public class YowyobPaymentApplication {

    /**
     * Lance l'application Spring Boot.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(YowyobPaymentApplication.class, args);
    }
}
