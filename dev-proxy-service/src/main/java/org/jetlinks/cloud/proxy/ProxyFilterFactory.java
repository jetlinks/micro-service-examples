package org.jetlinks.cloud.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
public class ProxyFilterFactory extends AbstractGatewayFilterFactory<ProxyFilterFactory.Config> {

    public ProxyFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("prefix");
    }

    @Override
    public GatewayFilter apply(Config config) {

        return new ProxyGatewayFilter();
    }

    @AllArgsConstructor
    static class ProxyGatewayFilter implements GatewayFilter, Ordered {
        @Override
        @SneakyThrows
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String host = Optional
                    .ofNullable(exchange.getRequest().getHeaders().getFirst("X-Service-Host"))
                    .orElse(exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST));
            if (StringUtils.isEmpty(host)) {
                return Mono.error(new UnsupportedOperationException("no host header"));
            }

            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            if (route == null) {
                log.warn("proxy no route for {}", exchange.getRequest().getURI());
                return chain.filter(exchange);
            }
            String[] hosts = host.split("[.]");
            String[] domainAndPort = hosts[0].split("[-]");

            String domain = String.join(".", Arrays.copyOfRange(domainAndPort, 0, domainAndPort.length - 1));

            String url = "http://" + domain + ":" + domainAndPort[domainAndPort.length - 1];
            log.debug("proxy {} to {}", exchange.getRequest().getURI(), url);
            Route newRoute = Route.async()
                    .id(route.getId())
                    .asyncPredicate(route.getPredicate())
                    .filters(route.getFilters())
                    .order(route.getOrder())
                    .uri(url)
                    .build();

            exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, newRoute);


            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER - 1;
        }
    }

    @Getter
    @Setter
    public static class Config {
        private String prefix;
    }
}
