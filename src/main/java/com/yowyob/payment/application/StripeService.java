package com.yowyob.payment.application;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.yowyob.payment.domain.transaction.Transaction;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Intégration Stripe Checkout Sessions.
 */
@Service
@RequiredArgsConstructor
public class StripeService {

        @Value("${yowyob.stripe.secret-key}")
        private String secretKey;

        @Value("${yowyob.stripe.success-url}")
        private String successUrl;

        @Value("${yowyob.stripe.cancel-url}")
        private String cancelUrl;

        @Value("${yowyob.stripe.currency}")
        private String currency;

        /**
         * Initialise le SDK Stripe avec la clé secrète.
         */
        @PostConstruct
        public void init() {
                Stripe.apiKey = secretKey;
        }

        /**
         * @param transaction transaction liée
         * @return session Checkout créée
         */
        public Mono<Session> createCheckoutSession(Transaction transaction) {
                long amountCents = transaction.amount().add(transaction.fees())
                                .multiply(BigDecimal.valueOf(100)).longValue();
                return Mono.fromCallable(() -> {
                        SessionCreateParams params = SessionCreateParams.builder()
                                        .setMode(SessionCreateParams.Mode.PAYMENT)
                                        .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                                        .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")
                                        .setCustomerCreation(SessionCreateParams.CustomerCreation.ALWAYS)
                                        .setBillingAddressCollection(
                                                        SessionCreateParams.BillingAddressCollection.REQUIRED)
                                        .putMetadata("transaction_id", transaction.id().toString())
                                        .putMetadata("reference", transaction.reference())
                                        .addLineItem(SessionCreateParams.LineItem.builder()
                                                        .setQuantity(1L)
                                                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                                                        .setCurrency(currency)
                                                                        .setUnitAmount(amountCents)
                                                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                                                                        .builder()
                                                                                        .setName("Paiement "
                                                                                                        + transaction.reference())
                                                                                        .build())
                                                                        .build())
                                                        .build())
                                        .build();
                        return Session.create(params);
                }).subscribeOn(Schedulers.boundedElastic());
        }

        /**
         * @param sessionId identifiant session Checkout
         * @return session Stripe
         */
        public Mono<Session> retrieveCheckoutSession(String sessionId) {
                return Mono.fromCallable(() -> Session.retrieve(sessionId))
                                .subscribeOn(Schedulers.boundedElastic());
        }
}
