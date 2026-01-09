package com.cityflow.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        final String finalRequestId = requestId;
        
        // Add request ID to response headers
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        long startTime = System.currentTimeMillis();
        
        log.info("[{}] {} {} - Started",
                requestId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath());

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[{}] {} {} - Completed in {}ms with status {}",
                            finalRequestId,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath(),
                            duration,
                            exchange.getResponse().getStatusCode());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
