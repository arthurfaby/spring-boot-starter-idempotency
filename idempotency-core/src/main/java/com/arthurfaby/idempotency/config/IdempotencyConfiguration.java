package com.arthurfaby.idempotency.config;

import com.arthurfaby.idempotency.store.IdempotencyStore;
import com.arthurfaby.idempotency.store.InMemoryIdempotencyStore;
import com.arthurfaby.idempotency.web.IdempotencyFilter;
import com.arthurfaby.idempotency.web.IdempotencyInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the idempotency components into Spring MVC: the store, the interceptor
 * (registered on the MVC interceptor chain) and the caching filter (registered
 * with high precedence so it wraps the request/response early).
 */
@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyConfiguration {

    @Bean
    public IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    public IdempotencyInterceptor idempotencyInterceptor(IdempotencyStore store, IdempotencyProperties properties) {
        return new IdempotencyInterceptor(store, properties);
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyProperties properties) {
        FilterRegistrationBean<IdempotencyFilter> registration =
                new FilterRegistrationBean<>(new IdempotencyFilter(properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    public WebMvcConfigurer idempotencyWebMvcConfigurer(IdempotencyInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
