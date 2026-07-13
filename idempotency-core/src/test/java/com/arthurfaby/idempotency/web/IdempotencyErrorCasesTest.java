package com.arthurfaby.idempotency.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthurfaby.idempotency.annotation.Idempotent;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

@SpringBootTest(classes = IdempotencyErrorCasesTest.TestApplication.class)
@AutoConfigureMockMvc
class IdempotencyErrorCasesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestController controller;

    @Test
    void sameKeyWithDifferentBodyReturns422() throws Exception {
        String key = "mismatch-key";

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10}"))
                .andExpect(status().isOk());

        // same key, different body → the key was used for another request
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":999}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void concurrentRequestWithSameKeyReturns409() throws Exception {
        String key = "in-progress-key";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Request 1 enters the handler and blocks there → the key is now IN_PROGRESS.
            Future<?> first = executor.submit(() -> {
                mockMvc.perform(post("/slow")
                                .header("Idempotency-Key", key)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isOk());
                return null;
            });

            // Wait until request 1 is actually inside the handler.
            assertThat(controller.awaitEntered(2, TimeUnit.SECONDS)).isTrue();

            // Request 2, same key, while the first is still running → 409 Conflict.
            mockMvc.perform(post("/slow")
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict());

            controller.proceed(); // let request 1 finish
            first.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestController.class)
    static class TestApplication {}

    @RestController
    static class TestController {

        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch proceed = new CountDownLatch(1);

        @PostMapping("/payments")
        @Idempotent
        Map<String, Object> pay(@RequestBody Map<String, Object> body) {
            return Map.of("amount", body.get("amount"));
        }

        @PostMapping("/slow")
        @Idempotent
        Map<String, Object> slow() throws InterruptedException {
            entered.countDown(); // signal "I'm in the handler"
            proceed.await(); // ...then block until the test releases us
            return Map.of("status", "done");
        }

        boolean awaitEntered(long timeout, TimeUnit unit) throws InterruptedException {
            return entered.await(timeout, unit);
        }

        void proceed() {
            proceed.countDown();
        }
    }
}
