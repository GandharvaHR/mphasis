package com.example.transaction;

import com.example.transaction.model.*;
import com.example.transaction.repository.TransactionStore;
import com.example.transaction.service.TransactionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionProcessorTest {

    private TransactionStore store;
    private TransactionProcessor processor;

    @BeforeEach
    void setUp() {
        store = new TransactionStore();
        processor = new TransactionProcessor(store, 2);
    }

    private TransactionRequest request(String txn, String req, int seq, String account, BigDecimal amount) {
        return new TransactionRequest(txn, req, account, TransactionType.CREDIT, amount, "INR", seq);
    }

    @Test
    void duplicateRequestMustNotCreateSecondRecord() {
        var r = request("TXN-1", "REQ-1", 1, "ACC-1", BigDecimal.TEN);
        processor.submit(r);
        processor.submit(r);

        processor.processAvailable();

        assertEquals(1, processor.records().size());
        assertEquals(TransactionStatus.DUPLICATE, processor.records().get(0).getStatus());
    }

    @Test
    void outOfOrderSequenceIsPendingThenProcessed() {
        processor.submit(request("TXN-2", "REQ-2", 2, "ACC-1", BigDecimal.TEN));
        processor.processAvailable();

        assertEquals(TransactionStatus.PENDING, processor.records().get(0).getStatus());

        processor.submit(request("TXN-1", "REQ-1", 1, "ACC-1", BigDecimal.TEN));
        processor.processAvailable();

        assertEquals(TransactionStatus.PROCESSED,
                processor.records().stream()
                        .filter(r -> r.getRequest().transactionId().equals("TXN-1"))
                        .findFirst().orElseThrow().getStatus());

        assertEquals(TransactionStatus.PROCESSED,
                processor.records().stream()
                        .filter(r -> r.getRequest().transactionId().equals("TXN-2"))
                        .findFirst().orElseThrow().getStatus());
    }

    @Test
    void invalidAmountFails() {
        processor.submit(request("TXN-4", "REQ-4", 1, "ACC-1",
                BigDecimal.valueOf(-10)));
        processor.processAvailable();

        assertEquals(TransactionStatus.FAILED, processor.records().get(0).getStatus());
    }

    @Test
    void transientFailureIsRetriedWithoutDuplicateEffect() {
        processor.submit(request("TXN-5", "REQ-5", 1, "RETRY",
                BigDecimal.valueOf(500)));
        processor.processAvailable();

        assertEquals(TransactionStatus.RETRY_PENDING, processor.records().get(0).getStatus());

        processor.processAvailable();

        assertEquals(TransactionStatus.PROCESSED, processor.records().get(0).getStatus());
        assertEquals(1, processor.records().get(0).getRetryCount());
    }
}
