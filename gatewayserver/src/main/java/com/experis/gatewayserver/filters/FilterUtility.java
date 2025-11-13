package com.experis.gatewayserver.filters;

import com.experis.dbmanager.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.http.HttpHeaders;
import java.util.UUID;

@Component
public class FilterUtility {

    private static final Logger logger = LoggerFactory.getLogger(FilterUtility.class);

    public String getCorrelationId(HttpHeaders requestHeaders) {
        String correlationId = requestHeaders.getFirst(Constants.CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            logger.debug("X-Correlation-ID NOT FOUND in RequestTraceFilter, generating one {}",correlationId);
        }else{
            logger.debug("X-Correlation-ID FOUND in RequestTraceFilter {}",correlationId);
        }

        return correlationId;
    }

    public ServerWebExchange setRequestHeader(ServerWebExchange exchange, String name, String value) {
        return exchange.mutate().request(exchange.getRequest().mutate().header(name, value).build()).build();
    }

    public ServerWebExchange setCorrelationId(ServerWebExchange exchange, String correlationId) {
        return this.setRequestHeader(exchange, Constants.CORRELATION_ID_HEADER, correlationId);
    }

}
