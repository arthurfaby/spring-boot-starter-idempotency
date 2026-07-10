package com.arthurfaby.idempotency.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration bound from the {@code idempotency.*} properties. Immutable
 * (constructor-bound record); every value has a sensible default.
 *
 * @param enabled    master switch for the whole feature
 * @param headerName name of the idempotency key header
 * @param defaultTtl key retention when {@code @Idempotent} doesn't override it
 * @param methods    HTTP methods eligible for idempotency
 */
@ConfigurationProperties(prefix = "idempotency")
public record IdempotencyProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("Idempotency-Key") String headerName,
        @DefaultValue("24h") Duration defaultTtl,
        @DefaultValue({"POST", "PATCH"}) List<String> methods) {}
