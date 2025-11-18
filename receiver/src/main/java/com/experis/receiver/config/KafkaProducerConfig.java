package com.experis.receiver.config;

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

    private static final String INTERCEPTOR_CLASS = "com.experis.receiver.interceptor.CorrelationIdProducerInterceptor";

    @Bean
    public DefaultKafkaProducerFactoryCustomizer producerFactoryCustomizer() {
        return producerFactory -> {
            if (producerFactory instanceof DefaultKafkaProducerFactory) {
                DefaultKafkaProducerFactory<?, ?> factory = (DefaultKafkaProducerFactory<?, ?>) producerFactory;

                // Ottieni le configurazioni esistenti
                Map<String, Object> existingProps = new HashMap<>(factory.getConfigurationProperties());

                // Ottieni la lista degli interceptor, o creane una nuova
                List<String> interceptors = (List<String>) existingProps.getOrDefault(
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, new ArrayList<>()
                );

                // Aggiungi il nostro interceptor solo se non è già presente
                if (!interceptors.contains(INTERCEPTOR_CLASS)) {
                    interceptors.add(INTERCEPTOR_CLASS);
                }

                // Aggiorna la configurazione con la nuova lista di interceptor
                factory.updateConfigs(Map.of(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors));
            }
        };
    }
}
