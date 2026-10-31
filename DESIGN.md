# Design Note

## 1. Architecture

The solution separates API/input handling from processing logic and state storage.

- Controller: accepts requests and exposes processing/reporting endpoints.
- Processor: owns validation, idempotency, ordering and retry behavior.
- Store: maintains transaction state.
- Model: represents request and lifecycle state.

## 2. Idempotency

Two identifiers are considered:

- transactionId: business transaction identity.
- requestId: retry/request identity.

A request already known by either identifier is not allowed to create a new business transaction.

For production, the store must be durable and enforce uniqueness at database level.

## 3. Duplicate detection

Duplicate requestId or transactionId is detected before processing. The existing record is marked DUPLICATE and no new business effect is performed.

## 4. Out-of-order handling

Sequence-based processing was selected because the assessment explicitly provides sequence numbers.

Example:

1. sequence 2 arrives -> PENDING
2. sequence 1 arrives -> PROCESSED
3. next processing cycle -> sequence 2 PROCESSED

A missing sequence is visible through the pending state/message.

## 5. Retry/failure handling

A transient failure is simulated for accountId `RETRY`.

The first attempt becomes RETRY_PENDING. A later processing cycle retries it. After success it becomes PROCESSED.

If retry count reaches the configured maximum, the record becomes FAILED.

## 6. Consistency

The lightweight implementation keeps transaction state in one in-memory record. Therefore it cannot provide true distributed transaction guarantees.

Production version should use a database transaction and atomic state transitions. If an external side effect is required, an outbox pattern is recommended.

## 7. Observability

Logs identify transaction ID and sequence without logging sensitive customer information.

Production implementation should add:

- correlationId
- metrics by status
- processing latency
- retry count
- failure count
- queue lag
- tracing
- alerts

## 8. Concurrency

The assessment implementation uses synchronized processing to keep the example deterministic.

Production multi-instance processing should use database locking, optimistic versioning, queue partitioning, or another explicit ownership mechanism.

## 9. AI usage

AI assistance was used for boilerplate, tests and documentation. All generated logic was reviewed against the requirements and validated through automated tests.

## 10. Production next steps

- Replace in-memory store with PostgreSQL.
- Add unique constraints.
- Add transactional state transitions.
- Introduce queue adapter.
- Add DLQ.
- Add exponential backoff/jitter.
- Add metrics/tracing.
- Add authentication.
- Add integration and concurrency tests.
