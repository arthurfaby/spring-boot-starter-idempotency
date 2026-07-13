package com.arthurfaby.idempotency.sample;

import com.arthurfaby.idempotency.annotation.Idempotent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final AtomicInteger sequence = new AtomicInteger();
    private final List<String> ledger = new CopyOnWriteArrayList<>();

    @PostMapping("/payments")
    @Idempotent
    public ResponseEntity<Map<String, Object>> pay(@RequestBody PaymentRequest request) {
        String paymentId = "pay_" + sequence.incrementAndGet();
        ledger.add(paymentId + " charged " + request.amount() + " " + request.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "paymentId", paymentId,
                        "amount", request.amount(),
                        "currency", request.currency()));
    }

    @GetMapping("/ledger")
    public List<String> ledger() {
        return ledger;
    }

    public record PaymentRequest(long amount, String currency) {}
}
