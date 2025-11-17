package com.experis.receiver.interceptor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.util.Map;

import static com.experis.commons.constants.Constants.CORRELATION_ID_HEADER;
import static com.experis.commons.constants.Constants.CORRELATION_ID_MDC_KEY;

public class CorrelationIdProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    @Override
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        if (correlationId != null && !correlationId.isEmpty()) {
            record.headers().add(CORRELATION_ID_HEADER, correlationId.getBytes());
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
