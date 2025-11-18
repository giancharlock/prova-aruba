package com.experis.gatewayserver.filters;

import com.experis.commons.constants.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Order(1)
@Component
public class RequestTraceFilter implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Autowired
    FilterUtility filterUtility;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        String correlationID = filterUtility.getEexistingCorrelatinId(requestHeaders);
        if (correlationID!=null) {
            logger.debug("invoiceApp-correlation-id found in RequestTraceFilter : {}",correlationID);
        } else {
            correlationID = filterUtility.getCorrelationId();
            exchange = filterUtility.setCorrelationId(exchange, correlationID);
            logger.debug("invoiceApp-correlation-id generated in RequestTraceFilter : {}", correlationID);
        }
        return chain.filter(exchange).contextWrite(Context.of(Constants.CORRELATION_ID_MDC_KEY, correlationID));
    }

}
