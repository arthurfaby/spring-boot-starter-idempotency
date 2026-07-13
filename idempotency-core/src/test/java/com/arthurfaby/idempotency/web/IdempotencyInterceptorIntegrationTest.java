package com.arthurfaby.idempotency.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthurfaby.idempotency.annotation.Idempotent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = IdempotencyInterceptorIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class IdempotencyInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentController controller;

    @BeforeEach
    void resetCounter() {
        controller.reset();
    }

    @Test
    void firstCallExecutesHandlerAndReplayDoesNot() throws Exception {
        String key = "key-123";
        String body = "{\"amount\":100}";

        // 1st call → executes the handler
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment-1"));

        // 2nd call, same key + body → replays the stored response
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("payment-1"));

        assertThat(controller.invocations()).isEqualTo(1);
    }

    @Test
    void withoutKeyHeaderTheHandlerRunsEveryTime() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\":100}"))
                    .andExpect(status().isOk());
        }
        assertThat(controller.invocations()).isEqualTo(2); // no key → runs each time
    }

    @Test
    void customTtlEndpointReplaysOnRetry() throws Exception {
        String key = "ttl-key";
        String body = "{\"amount\":5}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/with-ttl")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value("payment-1"));
        }
        assertThat(controller.invocations()).isEqualTo(1); // replayed → runs once
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PaymentController.class)
    static class TestApplication {}

    @RestController
    static class PaymentController {

        private final AtomicInteger counter = new AtomicInteger();

        @PostMapping("/payments")
        @Idempotent
        Map<String, Object> pay(@RequestBody Map<String, Object> body) {
            int number = counter.incrementAndGet();
            return Map.of("paymentId", "payment-" + number, "amount", body.get("amount"));
        }

        @PostMapping("/with-ttl")
        @Idempotent(ttl = "2h")
        Map<String, Object> withCustomTtl(@RequestBody Map<String, Object> body) {
            int number = counter.incrementAndGet();
            return Map.of("paymentId", "payment-" + number, "amount", body.get("amount"));
        }

        void reset() {
            counter.set(0);
        }

        int invocations() {
            return counter.get();
        }
    }
}
