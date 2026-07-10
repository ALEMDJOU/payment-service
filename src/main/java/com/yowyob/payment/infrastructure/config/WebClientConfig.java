package com.yowyob.payment.infrastructure.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * Beans HTTP pour livraison webhooks.
 */
@Configuration
public class WebClientConfig {

    /**
     * @param properties configuration webhook
     * @return client HTTP dédié aux callbacks consommateurs
     */
    @Bean
    public WebClient consumerWebhookWebClient(WebhookProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getTimeoutMs());
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
