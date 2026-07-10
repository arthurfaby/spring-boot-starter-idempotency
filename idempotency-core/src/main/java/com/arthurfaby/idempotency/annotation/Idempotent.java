package com.arthurfaby.idempotency.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller handler method as idempotent: repeated requests carrying the
 * same {@code Idempotency-Key} header are executed once and their response replayed.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Key retention as a Spring duration string (e.g. {@code "1h"}, {@code "30m"}, {@code "PT15S"}).
     * Empty means: use {@code idempotency.default-ttl}.
     */
    String ttl() default "";
}
