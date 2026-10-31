package com.example.transaction.controller;

import com.example.transaction.model.TransactionRecord;
import com.example.transaction.model.TransactionRequest;
import com.example.transaction.model.TransactionStatus;
import com.example.transaction.service.TransactionProcessor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionProcessor processor;

    public TransactionController(TransactionProcessor processor) {
        this.processor = processor;
    }

    @PostMapping
    public ResponseEntity<TransactionRecord> submit(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(processor.submit(request));
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process() {
        processor.processAvailable();
        return ResponseEntity.ok(Map.of(
                "message", "Processing completed",
                "summary", processor.summary()
        ));
    }

    @GetMapping
    public List<TransactionRecord> records() {
        return processor.records();
    }

    @GetMapping("/summary")
    public Map<TransactionStatus, Long> summary() {
        return processor.summary();
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Void> reset() {
        processor.clear();
        return ResponseEntity.noContent().build();
    }
}
