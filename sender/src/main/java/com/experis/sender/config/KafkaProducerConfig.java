package com.experis.sender.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaProducerConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ProducerFactory) {
            ProducerFactory<?, ?> producerFactory = (ProducerFactory<?, ?>) bean;
            Map<String, Object> props = producerFactory.getConfigurationProperties();

            List<String> interceptors = (List<String>) props.getOrDefault(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, new ArrayList<>());
            if (!interceptors.contains("com.experis.sender.interceptor.CorrelationIdProducerInterceptor")) {
                interceptors.add("com.experis.sender.interceptor.CorrelationIdProducerInterceptor");
            }

            props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
        }
        return bean;
    }
}
