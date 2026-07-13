package com.arthurfaby.idempotency.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.arthurfaby.idempotency.store.IdempotencyStore;
import com.arthurfaby.idempotency.store.InMemoryIdempotencyStore;
import com.arthurfaby.idempotency.web.IdempotencyInterceptor;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class IdempotencyAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class));

    @Test
    void registersDefaultBeansInAServletWebApp() {
        runner.run(context -> {
            IdempotencyProperties properties = context.getBean(IdempotencyProperties.class);
            assertThat(context).hasSingleBean(IdempotencyStore.class);
            assertThat(context).hasSingleBean(IdempotencyInterceptor.class);
            assertThat(context).getBean(IdempotencyStore.class).isInstanceOf(InMemoryIdempotencyStore.class);
            assertThat(properties.headerName()).isEqualTo("Idempotency-Key");
            assertThat(properties.defaultTtl()).isEqualTo(Duration.ofHours(24));
        });
    }

    @Test
    void backsOffWhenDisabled() {
        runner.withPropertyValues("idempotency.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(IdempotencyStore.class);
            assertThat(context).doesNotHaveBean(IdempotencyInterceptor.class);
        });
    }

    @Test
    void bindsCustomProperties() {
        runner.withPropertyValues("idempotency.header-name=X-Idem-Key", "idempotency.default-ttl=1h")
                .run(context -> {
                    IdempotencyProperties properties = context.getBean(IdempotencyProperties.class);
                    assertThat(properties.headerName()).isEqualTo("X-Idem-Key");
                    assertThat(properties.defaultTtl()).isEqualTo(Duration.ofHours(1));
                });
    }

    @Test
    void userDefinedStoreOverridesTheDefault() {
        runner.withUserConfiguration(CustomStoreConfig.class).run(context -> {
            assertThat(context).hasSingleBean(IdempotencyStore.class);
            assertThat(context).getBean(IdempotencyStore.class).isSameAs(CustomStoreConfig.CUSTOM_STORE);
        });
    }

    @Configuration
    static class CustomStoreConfig {

        static final IdempotencyStore CUSTOM_STORE = new InMemoryIdempotencyStore();

        @Bean
        IdempotencyStore idempotencyStore() {
            return CUSTOM_STORE;
        }
    }
}
