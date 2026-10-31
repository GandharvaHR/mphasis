package com.example.transaction.model;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionRecord {
    private final TransactionRequest request;
    private TransactionStatus status;
    private int retryCount;
    private String message;
    private final Instant receivedAt;

    public TransactionRecord(TransactionRequest request) {
        this.request = request;
        this.status = TransactionStatus.RECEIVED;
        this.receivedAt = Instant.now();
    }

    public TransactionRequest getRequest() { return request; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { this.retryCount++; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getReceivedAt() { return receivedAt; }
    public BigDecimal getAmount() { return request.amount(); }
}
