package com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.yowyob.payment.domain.transaction.PaymentMethod;
import com.yowyob.payment.domain.transaction.Transaction;
import com.yowyob.payment.domain.transaction.TransactionStatus;
import com.yowyob.payment.domain.transaction.TransactionType;
import com.yowyob.payment.domain.wallet.Wallet;
import com.yowyob.payment.domain.wallet.WalletStatus;
import com.yowyob.payment.domain.webhook.ConsumerWebhookEventType;
import com.yowyob.payment.domain.webhook.WebhookOutboxEntry;
import com.yowyob.payment.domain.webhook.WebhookOutboxStatus;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.mapper.PersistenceMapper;
import com.yowyob.payment.infrastructure.adapters.outbound.persistence.r2dbc.repository.WalletR2dbcRepository;
import com.yowyob.payment.infrastructure.config.R2dbcConfig;

import io.r2dbc.spi.ConnectionFactory;
import reactor.test.StepVerifier;

/**
 * Intégration R2DBC : persistance transaction avec callback_url et metadata (JSONB).
 */
@Testcontainers
@DataR2dbcTest
@ActiveProfiles("test")
@Import({ TransactionR2dbcAdapter.class, WebhookOutboxR2dbcAdapter.class, R2dbcConfig.class })
class TransactionR2dbcAdapterIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("yowyob_payment_it")
            .withUsername("yowyob")
            .withPassword("secret");

    @DynamicPropertySource
    static void registerR2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url",
                () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getFirstMappedPort()
                        + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.r2dbc.pool.enabled", () -> "false");
    }

    @Autowired
    private TransactionR2dbcAdapter transactionAdapter;

    @Autowired
    private WebhookOutboxR2dbcAdapter webhookOutboxAdapter;

    @Autowired
    private WalletR2dbcRepository walletRepository;

    @Autowired
    private ConnectionFactory connectionFactory;

    private UUID walletId;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUpSchemaAndWallet() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema/r2dbc-it-schema.sql"));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.populate(connectionFactory).block();

        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(walletId, userId, organizationId, BigDecimal.ZERO, WalletStatus.ACTIVE,
                Instant.now(), Instant.now());
        walletRepository.save(PersistenceMapper.toNewEntity(wallet)).block();
    }

    @Test
    void saveShouldPersistCallbackUrlAndMetadata() {
        String callbackUrl = "https://consumer.example/webhooks/payment";
        Map<String, String> metadata = Map.of("orderId", "ORD-42", "source", "integration-test");
        Transaction transaction = new Transaction(UUID.randomUUID(), walletId, userId, organizationId,
                new BigDecimal("1500.0000"), TransactionType.RECHARGE, TransactionStatus.CREATED,
                "YYPAY-IT-001", BigDecimal.ZERO, PaymentMethod.STRIPE, null, callbackUrl, metadata,
                Instant.now(), Instant.now());

        StepVerifier.create(transactionAdapter.save(transaction))
                .assertNext(saved -> {
                    assertNotNull(saved.id());
                    assertEquals(callbackUrl, saved.callbackUrl());
                    assertEquals(metadata, saved.metadata());
                })
                .verifyComplete();

        StepVerifier.create(transactionAdapter.findById(transaction.id()))
                .assertNext(loaded -> {
                    assertEquals(callbackUrl, loaded.callbackUrl());
                    assertEquals("ORD-42", loaded.metadata().get("orderId"));
                    assertEquals("integration-test", loaded.metadata().get("source"));
                })
                .verifyComplete();
    }

    @Test
    void saveWebhookOutboxShouldPersistJsonPayload() {
        Transaction transaction = new Transaction(UUID.randomUUID(), walletId, userId, organizationId,
                new BigDecimal("100.0000"), TransactionType.RECHARGE, TransactionStatus.CREATED,
                "YYPAY-IT-WEBHOOK", BigDecimal.ZERO, PaymentMethod.STRIPE, null, null, Map.of(),
                Instant.now(), Instant.now());
        transactionAdapter.save(transaction).block();

        String payloadJson = "{\"transactionId\":\"" + transaction.id() + "\",\"status\":\"PENDING\"}";
        WebhookOutboxEntry entry = new WebhookOutboxEntry(UUID.randomUUID(), transaction.id(),
                ConsumerWebhookEventType.TRANSACTION_PENDING, "https://consumer.example/hook", payloadJson,
                WebhookOutboxStatus.PENDING, 0, Instant.now(), null, Instant.now(), Instant.now());

        StepVerifier.create(webhookOutboxAdapter.save(entry))
                .assertNext(saved -> assertNotNull(saved.id()))
                .verifyComplete();

        StepVerifier.create(webhookOutboxAdapter.findById(entry.id()))
                .assertNext(loaded -> assertEquals(payloadJson, loaded.payloadJson()))
                .verifyComplete();
    }
}
