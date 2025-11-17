package com.experis.sender.interceptor;

import io.opentelemetry.api.trace.Span;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import static com.experis.commons.constants.Constants.*;

@Component
public class CorrelationIdInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        String correlationId = (String) message.getHeaders().get(CORRELATION_ID_HEADER);

        if (correlationId != null && !correlationId.isEmpty()) {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            Span.current().setAttribute(CORRELATION_ID_OTEL_ATTRIBUTE, correlationId);
        }
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}
