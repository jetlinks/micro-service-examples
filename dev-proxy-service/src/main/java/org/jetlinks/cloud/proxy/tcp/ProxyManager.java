package org.jetlinks.cloud.proxy.tcp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@ConfigurationProperties("proxy")
@Component
public class ProxyManager {

    private Frp frp;

    private final Map<InetSocketAddress, Cache> proxies = new ConcurrentHashMap<>();

    private Duration expires = Duration.ofMinutes(10);

    public ProxyManager() {

        if (!expires.isNegative() && !expires.isZero()) {
            Flux.interval(Duration.ofSeconds(30))
                .subscribe(l -> {
                    for (Map.Entry<InetSocketAddress, Cache> entry : proxies.entrySet()) {
                        if (System.currentTimeMillis() - entry.getValue().lastAccess > expires.toMillis()) {
                            entry.getValue().proxy.dispose();
                        }
                    }
                });
        }
    }

    public Mono<Proxy> startProxy(InetSocketAddress address) {
        if (frp == null) {
            throw new IllegalStateException("proxy unsupported");
        }
        return Mono.just(
                proxies.compute(address, (key, old) -> {
                    if (old != null && !old.proxy.isDisposed()) {
                        old.lastAccess = System.currentTimeMillis();
                        return old;
                    }
                    Cache proxy = new Cache(frp.startProxy(address));
                    proxy.proxy.doOnDispose(() -> proxies.remove(key, proxy));
                    return proxy;
                })
                        .proxy
        );
    }

    private static class Cache {
        Proxy proxy;

        private long lastAccess = System.currentTimeMillis();

        public Cache(Proxy proxy) {
            this.proxy = proxy;
        }
    }


}
