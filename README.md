<<<<<<< HEAD
# Idempotent Background Transaction Processing Service

## FDE L2 Practical Assessment

This project demonstrates an idempotent background transaction processor that handles:

- Duplicate requests
- Duplicate transaction IDs
- Out-of-order sequence numbers
- Retryable transient failures
- Retry exhaustion
- Invalid transactions
- Transaction status lifecycle
- Processing summary
- Unit tests
- Structured operational logging

## Technology

- Java 17
- Spring Boot 3.5.x
- Maven
- Spring Web
- Bean Validation
- JUnit 5

## Architecture

Request -> REST API -> Validation -> Transaction Store -> Background Processor

The transaction store acts as an in-memory idempotency/state store for the assessment.
For production, it should be replaced with a durable database and unique constraints.

## Run

```bash
mvn clean test
mvn spring-boot:run
```

Application starts on port 8080.

## Submit a transaction

```bash
curl -X POST http://localhost:8080/api/transactions ^
  -H "Content-Type: application/json" ^
  -d "{"transactionId":"TXN-2001","requestId":"REQ-1","accountId":"ACC-201","transactionType":"CREDIT","amount":100,"currency":"INR","sequenceNumber":1}"
```

## Process

```bash
curl -X POST http://localhost:8080/api/transactions/process
```

## View records

```bash
curl http://localhost:8080/api/transactions
```

## View summary

```bash
curl http://localhost:8080/api/transactions/summary
```

## Reset

```bash
curl -X DELETE http://localhost:8080/api/transactions/reset
```

## Idempotency design

The service tracks both transactionId and requestId. A duplicate request does not create another transaction record or execute the business effect again.

In production, this must be enforced at the persistence layer using unique constraints and an atomic insert/update pattern. In a multi-instance deployment, an in-memory map is not sufficient.

## Out-of-order strategy

Sequence-based processing is used. If sequence 2 arrives before sequence 1, sequence 2 becomes PENDING. Once sequence 1 is available and processed, the next processing cycle can process sequence 2.

This approach is simple and easy to explain for the assessment. A production system may use a durable pending-event store, queue partitioning by account, or state/version checks.

## Retry strategy

The implementation simulates a transient failure when accountId is `RETRY`.

- First attempt -> RETRY_PENDING
- Second attempt -> PROCESSED

Maximum attempts are configurable using `app.retry.max-attempts`.

Retries are safe because the transaction is identified by transactionId/requestId and the business effect is only applied after validation succeeds.

## Observability

The service logs:

- Transaction received
- Duplicate detected
- Transaction processed
- Retry scheduled
- Retry exhausted
- Business/validation failure

Production hardening would add correlation IDs, structured JSON logs, counters, latency metrics, tracing, and dashboards.

## Production hardening

1. PostgreSQL/MySQL for durable state.
2. Unique constraint on transactionId and idempotencyKey/requestId.
3. Atomic state transition using optimistic locking.
4. Queue such as Kafka/SQS/RabbitMQ.
5. Dead-letter queue for retry exhaustion.
6. Exponential backoff with jitter.
7. Distributed tracing and metrics.
8. Authentication/authorization.
9. Encryption and secret management.
10. Horizontal scaling with partitioning/locking strategy.
11. Durable pending-event storage for missing sequences.
12. Database transaction around business state and idempotency record.
13. API rate limiting and payload limits.
14. Alerting for retry spikes and failed transactions.

## AI-assisted development

AI tools were used for scaffolding, test generation, refactoring suggestions, and documentation. Generated code was reviewed and validated by running the test suite and manually checking the idempotency, ordering, and retry logic.

## Important trade-off

The assessment allows lightweight persistence. Therefore this implementation intentionally uses an in-memory store to keep the solution focused on the processing logic. The design note explicitly identifies the changes required before production use.
=======
# mphasis
Task
>>>>>>> a87cbd389f820c14a571ac9da43a84bd2005b2b3
