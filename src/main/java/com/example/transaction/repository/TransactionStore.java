package com.example.transaction.repository;

import com.example.transaction.model.TransactionRecord;
import com.example.transaction.model.TransactionStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TransactionStore {
    private final Map<String, TransactionRecord> byTransactionId = new ConcurrentHashMap<>();
    private final Map<String, String> requestToTransaction = new ConcurrentHashMap<>();

    public boolean containsTransaction(String transactionId) {
        return byTransactionId.containsKey(transactionId);
    }

    public boolean containsRequest(String requestId) {
        return requestToTransaction.containsKey(requestId);
    }

    public TransactionRecord save(TransactionRecord record) {
        byTransactionId.put(record.getRequest().transactionId(), record);
        requestToTransaction.put(record.getRequest().requestId(), record.getRequest().transactionId());
        return record;
    }

    public TransactionRecord find(String transactionId) {
        return byTransactionId.get(transactionId);
    }

    public List<TransactionRecord> findAll() {
        return new ArrayList<>(byTransactionId.values()).stream()
                .sorted(Comparator.comparingInt(r -> r.getRequest().sequenceNumber()))
                .toList();
    }

    public Map<TransactionStatus, Long> summary() {
        Map<TransactionStatus, Long> result = new java.util.EnumMap<>(TransactionStatus.class);
        for (TransactionStatus status : TransactionStatus.values()) {
            result.put(status, byTransactionId.values().stream()
                    .filter(r -> r.getStatus() == status).count());
        }
        return result;
    }

    public void clear() {
        byTransactionId.clear();
        requestToTransaction.clear();
    }
}
