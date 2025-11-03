package com.experis.scheduler.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Properties;

@Configuration
public class SchedulerConfig {

    /**
     * Bean WebClient LoadBalanced per interrogare dbmanager via Eureka.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Crea un KafkaConsumer manuale per il Job 1 (DLT).
     * Non usiamo @KafkaListener perché il README specifica un "job che gira",
     * implicando un polling controllato piuttosto che uno stream sempre attivo.
     */
    @Bean
    public KafkaConsumer<String, byte[]> dltKafkaConsumer(KafkaProperties kafkaProperties) {
        // Prende le proprietà da application.yml
        Map<String, Object> consumerProps = kafkaProperties.buildConsumerProperties(null);

        // Sovrascriviamo per assicurarci che non faccia auto-commit
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new KafkaConsumer<>(consumerProps);
    }
}