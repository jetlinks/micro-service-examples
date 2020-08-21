package org.jetlinks.cloud.proxy;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@SpringBootApplication
public class ProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }

    @Configuration
    public static class RouteConfiguration {

        @Bean
        public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
            //解决请求参数最大长度问题
            return factory -> factory.addServerCustomizers(httpServer ->
                    httpServer.httpRequestDecoder(spec -> {
                        spec.maxInitialLineLength((int) DataSize.parse(System.getProperty("server.max-initial-line-length","500KB")).toBytes());
                        spec.maxHeaderSize((int) DataSize.parse(System.getProperty("server.max-header-size","200KB")).toBytes());
                        return spec;
                    }));
        }

        @Bean
        public ProxyFilterFactory proxyFilterFactory() {
            return new ProxyFilterFactory();
        }

    }
}
