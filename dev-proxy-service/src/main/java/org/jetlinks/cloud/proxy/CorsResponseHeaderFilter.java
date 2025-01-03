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
import java.util.stream.Collectors;

public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {
    static final String[] duplicateKeys = {
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            HttpHeaders.ACCESS_CONTROL_MAX_AGE,
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return chain.filter(exchange)
                .then(Mono.fromRunnable(()->  removeDuplicateHeader(exchange)));
    }

    private void removeDuplicateHeader(ServerWebExchange exchange) {

        HttpHeaders headers = exchange.getResponse().getHeaders();
        if(headers.getClass().getName().contains("ReadOnlyHttpHeaders")){
            return;
        }

        List<String> vary =  headers.getVary().stream().distinct().collect(Collectors.toList());

        headers.remove(HttpHeaders.VARY);
        headers.addAll(HttpHeaders.VARY,vary);

        for (String duplicateKey : duplicateKeys) {
            List<String> keys = headers.get(duplicateKey);
            if (CollectionUtils.isEmpty(keys) || keys.size() == 1) {
                continue;
            }
            Set<String> distinct = new HashSet<>(keys);
            if(distinct.contains("*")){
                headers.set(duplicateKey,"*");
            }else {
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
