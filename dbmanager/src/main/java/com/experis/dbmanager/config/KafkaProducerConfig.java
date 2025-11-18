package com.experis.dbmanager.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private static final String INTERCEPTOR_CLASS = "com.experis.dbmanager.interceptor.CorrelationIdProducerInterceptor";

    @Bean
    public DefaultKafkaProducerFactoryCustomizer producerFactoryCustomizer() {
        return producerFactory -> {
            if (producerFactory instanceof DefaultKafkaProducerFactory) {
                DefaultKafkaProducerFactory<?, ?> factory = (DefaultKafkaProducerFactory<?, ?>) producerFactory;

                Map<String, Object> existingProps = new HashMap<>(factory.getConfigurationProperties());
                List<String> interceptors = (List<String>) existingProps.getOrDefault(
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, new ArrayList<>()
                );

                if (!interceptors.contains(INTERCEPTOR_CLASS)) {
                    interceptors.add(INTERCEPTOR_CLASS);
                }

                factory.updateConfigs(Map.of(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors));
            }
        };
    }
}
