package com.example.transaction.service;

import com.example.transaction.model.TransactionRecord;
import com.example.transaction.model.TransactionRequest;
import com.example.transaction.model.TransactionStatus;
import com.example.transaction.repository.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TransactionProcessor {
    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);

    private final TransactionStore store;
    private final int maxAttempts;

    public TransactionProcessor(TransactionStore store,
                                @Value("${app.retry.max-attempts:2}") int maxAttempts) {
        this.store = store;
        this.maxAttempts = maxAttempts;
    }

    public synchronized TransactionRecord submit(TransactionRequest request) {
        if (store.containsRequest(request.requestId())) {
            TransactionRecord existing = store.find(
                    store.findAll().stream()
                            .filter(r -> r.getRequest().requestId().equals(request.requestId()))
                            .map(r -> r.getRequest().transactionId())
                            .findFirst().orElse(request.transactionId()));
            existing.setStatus(TransactionStatus.DUPLICATE);
            existing.setMessage("Duplicate requestId ignored");
            log.info("Duplicate request detected requestId={}", request.requestId());
            return existing;
        }

        if (store.containsTransaction(request.transactionId())) {
            TransactionRecord existing = store.find(request.transactionId());
            existing.setStatus(TransactionStatus.DUPLICATE);
            existing.setMessage("Duplicate transactionId ignored");
            log.info("Duplicate transaction detected transactionId={}", request.transactionId());
            return existing;
        }

        TransactionRecord record = new TransactionRecord(request);
        store.save(record);
        log.info("Transaction received transactionId={} sequence={}",
                request.transactionId(), request.sequenceNumber());
        return record;
    }

    public synchronized void processAvailable() {
        List<TransactionRecord> records = store.findAll();
        int expected = 1;

        for (TransactionRecord record : records) {
            if (record.getStatus() == TransactionStatus.DUPLICATE ||
                    record.getStatus() == TransactionStatus.PROCESSED ||
                    record.getStatus() == TransactionStatus.FAILED) {
                continue;
            }

            int sequence = record.getRequest().sequenceNumber();

            if (sequence > expected) {
                record.setStatus(TransactionStatus.PENDING);
                record.setMessage("Waiting for sequence " + expected);
                continue;
            }

            if (sequence < expected) {
                record.setStatus(TransactionStatus.DUPLICATE);
                record.setMessage("Stale sequence ignored");
                continue;
            }

            processWithRetry(record);
            if (record.getStatus() == TransactionStatus.PROCESSED) {
                expected++;
            }
        }
    }

    private void processWithRetry(TransactionRecord record) {
        record.setStatus(TransactionStatus.PROCESSING);

        while (record.getRetryCount() < maxAttempts) {
            try {
                validateBusinessRule(record);
                record.setStatus(TransactionStatus.PROCESSED);
                record.setMessage("Transaction processed successfully");
                log.info("Transaction processed transactionId={}", record.getRequest().transactionId());
                return;
            } catch (TransientProcessingException ex) {
                record.incrementRetryCount();
                if (record.getRetryCount() < maxAttempts) {
                    record.setStatus(TransactionStatus.RETRY_PENDING);
                    record.setMessage("Transient failure; retry " + record.getRetryCount());
                    log.warn("Retry scheduled transactionId={} attempt={}",
                            record.getRequest().transactionId(), record.getRetryCount());
                } else {
                    record.setStatus(TransactionStatus.FAILED);
                    record.setMessage("Retry exhausted");
                    log.error("Retry exhausted transactionId={}", record.getRequest().transactionId());
                }
            } catch (IllegalArgumentException ex) {
                record.setStatus(TransactionStatus.FAILED);
                record.setMessage(ex.getMessage());
                log.warn("Validation/business failure transactionId={}",
                        record.getRequest().transactionId());
                return;
            }
        }
    }

    private void validateBusinessRule(TransactionRecord record) {
        TransactionRequest r = record.getRequest();

        if (r.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if ("RETRY".equalsIgnoreCase(r.accountId()) && record.getRetryCount() == 0) {
            throw new TransientProcessingException("Simulated transient downstream failure");
        }
    }

    public List<TransactionRecord> records() {
        return store.findAll();
    }

    public java.util.Map<TransactionStatus, Long> summary() {
        return store.summary();
    }

    public void clear() {
        store.clear();
    }

    static class TransientProcessingException extends RuntimeException {
        TransientProcessingException(String message) {
            super(message);
        }
    }
}
