package org.jetlinks.cloud.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class AuthConfiguration {

    @Bean
    public WebMvcConfigurer jwtAuthSupplier(JwtProperties properties) {
        JwtAuthSupplier supplier= new JwtAuthSupplier(properties);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(supplier);
            }
        };
    }

}
