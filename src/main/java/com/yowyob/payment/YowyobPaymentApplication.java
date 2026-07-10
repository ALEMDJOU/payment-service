package com.yowyob.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.yowyob.payment.infrastructure.config.KernelAuthProperties;
import com.yowyob.payment.infrastructure.config.WebhookProperties;

/**
 * Point d'entrée de l'API Yowyob Payment (wallets, transactions, Stripe).
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({ KernelAuthProperties.class, WebhookProperties.class })
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
