# TODOS — Wallet Project

Deferred items with context. Each entry explains WHY it was deferred, not just what it is.

---

## T1 — Self-transfer validation

**What:** Reject `POST /transfers` when `sourceAccountId == destinationAccountId` with HTTP 400.

**Why:** Currently the domain would debit and credit the same account (net-zero), leaving the balance unchanged but creating two phantom `Transaction` records in the history. This pollutes the audit trail and confuses users.

**Where to fix:** `TransferDomainService.transfer()` — add a guard at the top:
```java
if (source.getId().equals(destination.getId())) {
    throw new SelfTransferException(source.getId());
}
```
Add `SELF_TRANSFER` to `@ControllerAdvice` → 400.

**Depends on:** Nothing. One line in domain + one catch in ControllerAdvice + one test.

---

## T2 — POST /auth/token with non-existent email MUST return 401 (not 404)

**What:** When `POST /auth/token` is called with an email that isn't registered, return 401 Unauthorized — the same status as "wrong password."

**Why:** Returning 404 for a non-existent email leaks whether an account exists (email enumeration). An attacker can probe the API to discover which emails are registered. 401 for all auth failures is the secure industry standard.

**Where to fix:** In `@ControllerAdvice`, catch `UserNotFoundException` (or `UsernameNotFoundException` from Spring Security) → 401. Do NOT return 404 for user-not-found on login paths.

**Depends on:** UserRepository port implementation.

---

## T3 — GitHub Actions CI

**What:** `.github/workflows/ci.yml` running `./mvnw verify` on every push.

**Why:** Deferred by CEO review — user doesn't want it now. Easy to add after the core project works.

**Config:** `ubuntu-latest`, Java 21, `./mvnw verify` (runs unit + integration tests via H2).

---

## T4 — Domain Events (TransferExecutedEvent)

**What:** `Account` emits `TransferExecutedEvent` after a successful transfer. A listener persists the event or triggers side effects (notifications, audit log).

**Why:** Deferred to Phase 3. Would require re-opening domain event infrastructure that was explicitly CUT in CEO review. Better learned after core DDD is working.

---

## T5 — Spring Security roles/permissions

**What:** Role-based access control beyond basic authentication (e.g., ADMIN role for account management, USER role for transfers).

**Why:** V1 auth is stateless JWT with no roles. Adding roles before core features work adds unnecessary complexity.

---

## T6 — Rate limiting per account

**What:** Limit how many transfer requests a single account can make per minute.

**Why:** Not relevant for a learning/portfolio project. Add when hardening for production.

---

## T7 — Soft delete of accounts

**What:** `DELETE /accounts/{id}` marks an account as inactive rather than hard-deleting.

**Why:** No delete endpoint in v1 scope. Hard delete violates the immutable transaction history principle.

---

## T8 — Idempotency keys for transfers

**What:** Accept an `Idempotency-Key` header on `POST /transfers`. If the same key is received twice, return the original response without executing a second transfer.

**Why:** V1 doesn't implement this. A network retry creates a second transfer. Idempotency is a real fintech concern — great learning exercise for Phase 2.

**Schema:** Add `idempotency_key VARCHAR(64) UNIQUE` to `transfers` table (or a dedicated `idempotency_keys` table).

---

## T9 — Lock acquisition timeout

**What:** Configure `spring.jpa.properties.hibernate.lock.timeout` to prevent indefinitely-hanging transfer requests when the database is slow.

**Why:** `SELECT FOR UPDATE` with no timeout will block a transfer thread indefinitely if the database is overloaded. For a learning project this is acceptable, but the behavior should be documented.

**Note:** H2 doesn't support lock timeouts; this is a PostgreSQL production concern only.
