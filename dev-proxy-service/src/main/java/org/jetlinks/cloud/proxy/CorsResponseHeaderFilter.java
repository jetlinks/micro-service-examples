package org.jetlinks.cloud.proxy;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;

public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {
    static final String[] ACCESS_CONTROL_KEYS = {
            HttpHeaders.VARY,
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            HttpHeaders.ACCESS_CONTROL_MAX_AGE,
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        exchange
                .getResponse()
                .beforeCommit(
                        () -> {
                            removeDuplicateHeader(exchange);
                            return Mono.empty();
                        }
                );
        return chain.filter(exchange);
    }

    private void removeDuplicateHeader(ServerWebExchange exchange) {

        HttpHeaders headers = exchange.getResponse().getHeaders();

        for (String duplicateKey : ACCESS_CONTROL_KEYS) {
            List<String> keys = headers.get(duplicateKey);
            if (CollectionUtils.isEmpty(keys) || keys.size() == 1) {
                continue;
            }
            Set<String> distinct = new HashSet<>(keys);
            if (distinct.contains("*")) {
                headers.set(duplicateKey, "*");
            } else if (distinct.size() != keys.size()) {
                headers.remove(duplicateKey);
                headers.addAll(duplicateKey, new ArrayList<>(distinct));
            }
        }

    }

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER + 1;
    }
}
