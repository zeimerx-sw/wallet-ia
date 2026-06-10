# Wallet Digital — Hexagonal DDD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a PIX-like digital wallet REST API demonstrating DDD, Hexagonal Architecture, Spring Security JWT, OpenAPI docs, and pessimistic locking for concurrent transfers.

**Architecture:** Hexagonal (Ports & Adapters) with DDD rich domain. Domain model is pure Java with no framework dependencies. Spring, JPA, and Security live entirely in the adapter/infrastructure layers. `Account` aggregate holds business invariants; `TransferDomainService` orchestrates cross-aggregate operations.

**Tech Stack:** Java 21, Spring Boot 3.5.14, Spring Security 6, Spring Data JPA, H2 (tests), PostgreSQL (prod), jjwt 0.12.6, springdoc-openapi 2.8.9, JUnit 5, Mockito, AssertJ

---

## File Structure

```
src/main/java/io/github/zeimerxsw/wallet/
├── domain/
│   ├── model/
│   │   ├── AccountId.java          — UUID value object
│   │   ├── Money.java              — BigDecimal value object, always non-negative
│   │   ├── TransactionType.java    — enum DEBIT/CREDIT
│   │   ├── Transaction.java        — immutable record of one debit/credit event
│   │   └── Account.java            — aggregate root, holds balance + new transactions
│   ├── port/
│   │   └── out/
│   │       └── AccountRepository.java   — findById / findByIdForUpdate / save
│   ├── service/
│   │   └── TransferDomainService.java   — pure domain logic, no Spring
│   └── exception/
│       ├── InsufficientFundsException.java
│       ├── AccountNotFoundException.java
│       └── SameAccountTransferException.java
├── application/
│   ├── model/
│   │   ├── User.java               — auth user POJO (not domain)
│   │   ├── TransactionDetail.java  — read model record for history endpoint
│   │   ├── RegisterCommand.java
│   │   └── RegisterResult.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── TransferUseCase.java
│   │   │   ├── TransferCommand.java
│   │   │   └── RegisterUseCase.java
│   │   └── out/
│   │       ├── UserRepository.java          — save / findByEmail
│   │       └── TransactionQueryPort.java    — findByAccountId (pageable)
│   └── service/
│       ├── TransferService.java    — @Transactional, lock ordering for deadlock prevention
│       ├── RegisterService.java    — BCrypt + atomic User+Account creation
│       └── AccountService.java     — create / get / list transactions (thin orchestration)
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── AuthController.java
│   │       ├── AccountController.java
│   │       ├── TransferController.java
│   │       ├── GlobalExceptionHandler.java
│   │       └── dto/
│   │           ├── RegisterRequest.java
│   │           ├── RegisterResponse.java
│   │           ├── LoginRequest.java
│   │           ├── LoginResponse.java
│   │           ├── CreateTransferRequest.java
│   │           ├── AccountResponse.java
│   │           └── TransactionResponse.java
│   └── out/
│       └── persistence/
│           ├── AccountJpaEntity.java
│           ├── TransactionJpaEntity.java
│           ├── UserJpaEntity.java
│           ├── AccountJpaRepository.java
│           ├── TransactionJpaRepository.java
│           ├── UserJpaRepository.java
│           ├── AccountPersistenceAdapter.java   — implements AccountRepository
│           ├── TransactionPersistenceAdapter.java — implements TransactionQueryPort
│           ├── UserPersistenceAdapter.java       — implements UserRepository
│           └── mapper/
│               ├── AccountMapper.java
│               └── TransactionMapper.java
└── infrastructure/
    ├── security/
    │   ├── SecurityConfig.java
    │   ├── JwtAuthenticationFilter.java
    │   ├── JwtTokenProvider.java
    │   └── UserDetailsServiceImpl.java
    └── config/
        └── OpenApiConfig.java

src/main/resources/
├── application.yaml           — PostgreSQL config
└── application-local.yaml     — (gitignored) local overrides

src/test/resources/
└── application.yaml           — H2 in-memory config

src/test/java/io/github/zeimerxsw/wallet/
├── domain/model/AccountTest.java
├── application/service/
│   ├── TransferServiceTest.java
│   └── RegisterServiceTest.java
├── adapter/
│   ├── in/web/
│   │   ├── AuthControllerTest.java
│   │   ├── AccountControllerTest.java
│   │   └── ControllerAdviceTest.java
│   └── out/persistence/
│       └── AccountPersistenceAdapterTest.java
├── infrastructure/security/JwtTokenProviderTest.java
└── integration/ConcurrencyIT.java

docker-compose.yml
```

---

### Task 1: Maven Dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Replace the dependencies block in pom.xml**

Replace everything between `<dependencies>` and `</dependencies>` with:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.8.9</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

- [ ] **Step 2: Verify dependencies resolve**

```bash
./mvnw dependency:resolve -q
```
Expected: BUILD SUCCESS, no errors.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add web, jpa, security, jwt, springdoc, h2 dependencies"
```

---

### Task 2: Application Configuration

**Files:**
- Modify: `src/main/resources/application.yaml`
- Create: `src/test/resources/application.yaml`
- Modify: `.gitignore`

- [ ] **Step 1: Write main application.yaml**

Replace `src/main/resources/application.yaml` contents with:

```yaml
spring:
  application:
    name: wallet
  datasource:
    url: jdbc:postgresql://localhost:5432/wallet
    username: wallet
    password: wallet
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true

jwt:
  secret: ${JWT_SECRET:changeme-local-dev-secret-minimum-32ch}
  expiration-ms: 86400000
```

- [ ] **Step 2: Create test application.yaml**

Create `src/test/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
    show-sql: true

jwt:
  secret: test-secret-for-unit-tests-32-chars!!
  expiration-ms: 3600000
```

- [ ] **Step 3: Add application-local.yaml to .gitignore**

Append to `.gitignore`:
```
application-local.yaml
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yaml src/test/resources/application.yaml .gitignore
git commit -m "config: add main (postgres) and test (h2) application configs"
```

---

### Task 3: Domain Value Objects

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/model/Money.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/model/AccountId.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/model/TransactionType.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/model/Transaction.java`

- [ ] **Step 1: Write Money.java**

```java
package io.github.zeimerxsw.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("amount must be non-negative");
        this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(BigDecimal amount) { return new Money(amount); }
    public static Money of(String amount) { return new Money(new BigDecimal(amount)); }
    public static Money zero() { return new Money(BigDecimal.ZERO); }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("subtraction would result in negative amount");
        return new Money(result);
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public BigDecimal getAmount() { return amount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(amount.stripTrailingZeros()); }

    @Override
    public String toString() { return amount.toPlainString(); }
}
```

- [ ] **Step 2: Write AccountId.java**

```java
package io.github.zeimerxsw.wallet.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {
    private final UUID value;

    public AccountId(UUID value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static AccountId generate() { return new AccountId(UUID.randomUUID()); }
    public static AccountId of(UUID value) { return new AccountId(value); }
    public static AccountId of(String value) { return new AccountId(UUID.fromString(value)); }

    public UUID getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountId a)) return false;
        return value.equals(a.value);
    }

    @Override
    public int hashCode() { return Objects.hash(value); }

    @Override
    public String toString() { return value.toString(); }
}
```

- [ ] **Step 3: Write TransactionType.java**

```java
package io.github.zeimerxsw.wallet.domain.model;

public enum TransactionType { DEBIT, CREDIT }
```

- [ ] **Step 4: Write Transaction.java**

```java
package io.github.zeimerxsw.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final AccountId accountId;
    private final Money amount;
    private final TransactionType type;
    private final Instant createdAt;

    public Transaction(UUID id, AccountId accountId, Money amount, TransactionType type, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.amount = Objects.requireNonNull(amount);
        this.type = Objects.requireNonNull(type);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() { return id; }
    public AccountId getAccountId() { return accountId; }
    public Money getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 5: Compile to verify**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/domain/
git commit -m "feat: add domain value objects Money, AccountId, TransactionType, Transaction"
```

---

### Task 4: Account Aggregate + AccountTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/exception/InsufficientFundsException.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/model/Account.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/domain/model/AccountTest.java`

- [ ] **Step 1: Write InsufficientFundsException.java** (needed before Account can compile)

```java
package io.github.zeimerxsw.wallet.domain.exception;

import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(AccountId accountId, Money balance, Money requested) {
        super("Account " + accountId + " has balance " + balance + " but requested " + requested);
    }
}
```

- [ ] **Step 2: Write AccountTest.java (failing tests first)**

```java
package io.github.zeimerxsw.wallet.domain.model;

import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AccountTest {

    @Test
    void credit_updatesBalanceAndRecordsTransaction() {
        Account account = new Account(AccountId.generate(), Money.zero());
        account.credit(Money.of("100.00"));
        assertThat(account.getBalance()).isEqualTo(Money.of("100.00"));
        assertThat(account.getDomainTransactions()).hasSize(1);
        assertThat(account.getDomainTransactions().get(0).getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(account.getDomainTransactions().get(0).getAmount()).isEqualTo(Money.of("100.00"));
    }

    @Test
    void debit_withSufficientFunds_updatesBalanceAndRecordsTransaction() {
        Account account = new Account(AccountId.generate(), Money.of("100.00"));
        account.debit(Money.of("40.00"));
        assertThat(account.getBalance()).isEqualTo(Money.of("60.00"));
        assertThat(account.getDomainTransactions()).hasSize(1);
        assertThat(account.getDomainTransactions().get(0).getType()).isEqualTo(TransactionType.DEBIT);
    }

    @Test
    void debit_withInsufficientFunds_throwsAndLeavesBalanceUnchanged() {
        Account account = new Account(AccountId.generate(), Money.of("50.00"));
        assertThatThrownBy(() -> account.debit(Money.of("100.00")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.getBalance()).isEqualTo(Money.of("50.00"));
        assertThat(account.getDomainTransactions()).isEmpty();
    }

    @Test
    void debit_exactBalance_succeeds() {
        Account account = new Account(AccountId.generate(), Money.of("50.00"));
        account.debit(Money.of("50.00"));
        assertThat(account.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    void multipleOperations_accumulateTransactions() {
        Account account = new Account(AccountId.generate(), Money.of("100.00"));
        account.debit(Money.of("30.00"));
        account.credit(Money.of("20.00"));
        assertThat(account.getBalance()).isEqualTo(Money.of("90.00"));
        assertThat(account.getDomainTransactions()).hasSize(2);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./mvnw test -Dtest=AccountTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `Account` does not exist yet.

- [ ] **Step 4: Write Account.java**

```java
package io.github.zeimerxsw.wallet.domain.model;

import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Account {
    private final AccountId id;
    private Money balance;
    private final List<Transaction> domainTransactions = new ArrayList<>();

    public Account(AccountId id, Money balance) {
        this.id = Objects.requireNonNull(id);
        this.balance = Objects.requireNonNull(balance);
    }

    public void credit(Money amount) {
        this.balance = this.balance.add(amount);
        domainTransactions.add(new Transaction(UUID.randomUUID(), this.id, amount, TransactionType.CREDIT, Instant.now()));
    }

    public void debit(Money amount) {
        if (amount.isGreaterThan(balance)) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        domainTransactions.add(new Transaction(UUID.randomUUID(), this.id, amount, TransactionType.DEBIT, Instant.now()));
    }

    public AccountId getId() { return id; }
    public Money getBalance() { return balance; }
    public List<Transaction> getDomainTransactions() { return Collections.unmodifiableList(domainTransactions); }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./mvnw test -Dtest=AccountTest -q
```
Expected: Tests run: 5, Failures: 0, Errors: 0.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/domain/ src/test/java/io/github/zeimerxsw/wallet/domain/
git commit -m "feat: add Account aggregate with debit/credit invariants + AccountTest"
```

---

### Task 5: Domain Exceptions and Ports

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/exception/AccountNotFoundException.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/exception/SameAccountTransferException.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/port/out/AccountRepository.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/domain/service/TransferDomainService.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/in/TransferCommand.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/in/TransferUseCase.java`

- [ ] **Step 1: Write remaining domain exceptions**

`AccountNotFoundException.java`:
```java
package io.github.zeimerxsw.wallet.domain.exception;

import io.github.zeimerxsw.wallet.domain.model.AccountId;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId id) {
        super("Account not found: " + id);
    }
}
```

`SameAccountTransferException.java`:
```java
package io.github.zeimerxsw.wallet.domain.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Source and target accounts must be different");
    }
}
```

- [ ] **Step 2: Write AccountRepository port**

```java
package io.github.zeimerxsw.wallet.domain.port.out;

import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;

public interface AccountRepository {
    Account findById(AccountId id);
    Account findByIdForUpdate(AccountId id);
    void save(Account account);
}
```

- [ ] **Step 3: Write TransferDomainService.java**

```java
package io.github.zeimerxsw.wallet.domain.service;

import io.github.zeimerxsw.wallet.domain.exception.SameAccountTransferException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class TransferDomainService {
    public void transfer(Account source, Account target, Money amount) {
        if (source.getId().equals(target.getId())) {
            throw new SameAccountTransferException();
        }
        source.debit(amount);
        target.credit(amount);
    }
}
```

- [ ] **Step 4: Write TransferCommand and TransferUseCase**

`TransferCommand.java`:
```java
package io.github.zeimerxsw.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {}
```

`TransferUseCase.java`:
```java
package io.github.zeimerxsw.wallet.application.port.in;

public interface TransferUseCase {
    void transfer(TransferCommand command);
}
```

- [ ] **Step 5: Compile**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/domain/ src/main/java/io/github/zeimerxsw/wallet/application/
git commit -m "feat: add domain ports, exceptions, TransferDomainService, TransferUseCase"
```

---

### Task 6: User Auth Model and Application Ports

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/model/User.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/model/TransactionDetail.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/in/RegisterCommand.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/in/RegisterResult.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/in/RegisterUseCase.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/out/UserRepository.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/port/out/TransactionQueryPort.java`

- [ ] **Step 1: Write User.java** (auth POJO — per architecture decision A3, User is NOT domain)

```java
package io.github.zeimerxsw.wallet.application.model;

import java.util.UUID;

public class User {
    private final UUID id;
    private final String email;
    private final String passwordHash;

    public User(UUID id, String email, String passwordHash) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
```

- [ ] **Step 2: Write TransactionDetail.java**

```java
package io.github.zeimerxsw.wallet.application.model;

import io.github.zeimerxsw.wallet.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDetail(UUID id, BigDecimal amount, TransactionType type, Instant createdAt) {}
```

- [ ] **Step 3: Write register command / result / use case**

`RegisterCommand.java`:
```java
package io.github.zeimerxsw.wallet.application.port.in;

public record RegisterCommand(String email, String rawPassword) {}
```

`RegisterResult.java`:
```java
package io.github.zeimerxsw.wallet.application.port.in;

import java.util.UUID;

public record RegisterResult(UUID userId, UUID accountId) {}
```

`RegisterUseCase.java`:
```java
package io.github.zeimerxsw.wallet.application.port.in;

public interface RegisterUseCase {
    RegisterResult register(RegisterCommand command);
}
```

- [ ] **Step 4: Write UserRepository and TransactionQueryPort**

`UserRepository.java`:
```java
package io.github.zeimerxsw.wallet.application.port.out;

import io.github.zeimerxsw.wallet.application.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
}
```

`TransactionQueryPort.java`:
```java
package io.github.zeimerxsw.wallet.application.port.out;

import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionQueryPort {
    Page<TransactionDetail> findByAccountId(UUID accountId, Pageable pageable);
}
```

- [ ] **Step 5: Compile**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/application/
git commit -m "feat: add application layer — User, RegisterUseCase, UserRepository, TransactionQueryPort"
```

---

### Task 7: JPA Entities

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/AccountJpaEntity.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/TransactionJpaEntity.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/UserJpaEntity.java`

Note: Declare `private UUID id` without `@Column(columnDefinition = "uuid")`. Hibernate 6 maps UUID correctly for both H2 and PostgreSQL automatically (architecture decision A5).

- [ ] **Step 1: Write AccountJpaEntity.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
```

- [ ] **Step 2: Write TransactionJpaEntity.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.domain.model.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Write UserJpaEntity.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
```

- [ ] **Step 4: Compile**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/adapter/
git commit -m "feat: add JPA entities for Account, Transaction, User"
```

---

### Task 8: Account Persistence Adapter + DataJpaTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/AccountJpaRepository.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/TransactionJpaRepository.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/mapper/AccountMapper.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/mapper/TransactionMapper.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/AccountPersistenceAdapter.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/TransactionPersistenceAdapter.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/adapter/out/persistence/AccountPersistenceAdapterTest.java`

- [ ] **Step 1: Write AccountJpaRepository.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id = :id")
    Optional<AccountJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
```

- [ ] **Step 2: Write TransactionJpaRepository.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID> {
    Page<TransactionJpaEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
```

- [ ] **Step 3: Write AccountMapper.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence.mapper;

import io.github.zeimerxsw.wallet.adapter.out.persistence.AccountJpaEntity;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toDomain(AccountJpaEntity entity) {
        return new Account(AccountId.of(entity.getId()), Money.of(entity.getBalance()));
    }

    public AccountJpaEntity toJpa(Account account) {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(account.getId().getValue());
        entity.setBalance(account.getBalance().getAmount());
        return entity;
    }
}
```

- [ ] **Step 4: Write TransactionMapper.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence.mapper;

import io.github.zeimerxsw.wallet.adapter.out.persistence.TransactionJpaEntity;
import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import io.github.zeimerxsw.wallet.domain.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionJpaEntity toJpa(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setAccountId(transaction.getAccountId().getValue());
        entity.setAmount(transaction.getAmount().getAmount());
        entity.setType(transaction.getType());
        entity.setCreatedAt(transaction.getCreatedAt());
        return entity;
    }

    public TransactionDetail toDetail(TransactionJpaEntity entity) {
        return new TransactionDetail(entity.getId(), entity.getAmount(), entity.getType(), entity.getCreatedAt());
    }
}
```

- [ ] **Step 5: Write AccountPersistenceAdapterTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.AccountMapper;
import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.TransactionMapper;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AccountMapper.class, TransactionMapper.class})
class AccountPersistenceAdapterTest {

    @Autowired AccountJpaRepository accountJpaRepository;
    @Autowired TransactionJpaRepository transactionJpaRepository;
    @Autowired AccountMapper accountMapper;
    @Autowired TransactionMapper transactionMapper;

    AccountPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccountPersistenceAdapter(
                accountJpaRepository, transactionJpaRepository, accountMapper, transactionMapper);
    }

    @Test
    void save_newAccount_persistsToDatabase() {
        Account account = new Account(AccountId.generate(), Money.of("100.00"));
        adapter.save(account);
        assertThat(accountJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void save_persistsTransactionsViaTransactionRepository() {
        AccountJpaEntity entity = new AccountJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setBalance(new BigDecimal("100.00"));
        accountJpaRepository.save(entity);

        Account account = adapter.findById(AccountId.of(entity.getId()));
        account.debit(Money.of("30.00"));
        adapter.save(account);

        List<TransactionJpaEntity> transactions = transactionJpaRepository.findAll();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void findById_nonExistent_throwsAccountNotFoundException() {
        var id = AccountId.of(UUID.randomUUID());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.findById(id))
                .isInstanceOf(io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException.class);
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

```bash
./mvnw test -Dtest=AccountPersistenceAdapterTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `AccountPersistenceAdapter` does not exist yet.

- [ ] **Step 7: Write AccountPersistenceAdapter.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.AccountMapper;
import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.TransactionMapper;
import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;
    private final TransactionJpaRepository transactionJpaRepository;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    public AccountPersistenceAdapter(
            AccountJpaRepository accountJpaRepository,
            TransactionJpaRepository transactionJpaRepository,
            AccountMapper accountMapper,
            TransactionMapper transactionMapper) {
        this.accountJpaRepository = accountJpaRepository;
        this.transactionJpaRepository = transactionJpaRepository;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Account findById(AccountId id) {
        return accountJpaRepository.findById(id.getValue())
                .map(accountMapper::toDomain)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Override
    public Account findByIdForUpdate(AccountId id) {
        return accountJpaRepository.findByIdForUpdate(id.getValue())
                .map(accountMapper::toDomain)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Override
    public void save(Account account) {
        AccountJpaEntity entity = accountJpaRepository.findById(account.getId().getValue())
                .orElse(new AccountJpaEntity());
        entity.setId(account.getId().getValue());
        entity.setBalance(account.getBalance().getAmount());
        accountJpaRepository.save(entity);

        List<TransactionJpaEntity> newTransactions = account.getDomainTransactions()
                .stream()
                .map(transactionMapper::toJpa)
                .toList();
        if (!newTransactions.isEmpty()) {
            transactionJpaRepository.saveAll(newTransactions);
        }
    }
}
```

- [ ] **Step 8: Write TransactionPersistenceAdapter.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.adapter.out.persistence.mapper.TransactionMapper;
import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import io.github.zeimerxsw.wallet.application.port.out.TransactionQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionPersistenceAdapter implements TransactionQueryPort {

    private final TransactionJpaRepository transactionJpaRepository;
    private final TransactionMapper transactionMapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository transactionJpaRepository, TransactionMapper transactionMapper) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Page<TransactionDetail> findByAccountId(UUID accountId, Pageable pageable) {
        return transactionJpaRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(transactionMapper::toDetail);
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

```bash
./mvnw test -Dtest=AccountPersistenceAdapterTest -q
```
Expected: Tests run: 3, Failures: 0, Errors: 0.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/adapter/ src/test/java/io/github/zeimerxsw/wallet/adapter/
git commit -m "feat: add account/transaction persistence adapters + AccountPersistenceAdapterTest"
```

---

### Task 9: User Persistence Adapter

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/UserJpaRepository.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/UserPersistenceAdapter.java`

- [ ] **Step 1: Write UserJpaRepository.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
}
```

- [ ] **Step 2: Write UserPersistenceAdapter.java**

```java
package io.github.zeimerxsw.wallet.adapter.out.persistence;

import io.github.zeimerxsw.wallet.application.model.User;
import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        userJpaRepository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(e -> new User(e.getId(), e.getEmail(), e.getPasswordHash()));
    }
}
```

- [ ] **Step 3: Compile**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/UserJpaRepository.java
git add src/main/java/io/github/zeimerxsw/wallet/adapter/out/persistence/UserPersistenceAdapter.java
git commit -m "feat: add UserJpaRepository and UserPersistenceAdapter"
```

---

### Task 10: Transfer Use Case + TransferServiceTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/service/TransferService.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/application/service/TransferServiceTest.java`

- [ ] **Step 1: Write TransferServiceTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import io.github.zeimerxsw.wallet.domain.service.TransferDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accountRepository;

    TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountRepository, new TransferDomainService());
    }

    @Test
    void transfer_withSufficientFunds_updatesBalancesAndSavesBoth() {
        AccountId sourceId = AccountId.generate();
        AccountId targetId = AccountId.generate();
        Account source = new Account(sourceId, Money.of("100.00"));
        Account target = new Account(targetId, Money.zero());

        when(accountRepository.findByIdForUpdate(any())).thenAnswer(inv -> {
            AccountId id = inv.getArgument(0);
            return id.equals(sourceId) ? source : target;
        });

        transferService.transfer(new TransferCommand(sourceId.getValue(), targetId.getValue(), new BigDecimal("40.00")));

        assertThat(source.getBalance()).isEqualTo(Money.of("60.00"));
        assertThat(target.getBalance()).isEqualTo(Money.of("40.00"));
        verify(accountRepository, times(2)).save(any());
    }

    @Test
    void transfer_withInsufficientFunds_throwsAndDoesNotSave() {
        AccountId sourceId = AccountId.generate();
        AccountId targetId = AccountId.generate();
        Account source = new Account(sourceId, Money.of("10.00"));
        Account target = new Account(targetId, Money.zero());

        when(accountRepository.findByIdForUpdate(any())).thenAnswer(inv -> {
            AccountId id = inv.getArgument(0);
            return id.equals(sourceId) ? source : target;
        });

        assertThatThrownBy(() ->
                transferService.transfer(new TransferCommand(sourceId.getValue(), targetId.getValue(), new BigDecimal("100.00")))
        ).isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=TransferServiceTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `TransferService` does not exist yet.

- [ ] **Step 3: Write TransferService.java**

```java
package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.application.port.in.TransferUseCase;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import io.github.zeimerxsw.wallet.domain.service.TransferDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferService implements TransferUseCase {

    private final AccountRepository accountRepository;
    private final TransferDomainService transferDomainService;

    public TransferService(AccountRepository accountRepository, TransferDomainService transferDomainService) {
        this.accountRepository = accountRepository;
        this.transferDomainService = transferDomainService;
    }

    @Override
    public void transfer(TransferCommand command) {
        AccountId sourceId = AccountId.of(command.sourceAccountId());
        AccountId targetId = AccountId.of(command.targetAccountId());
        Money amount = Money.of(command.amount());

        // Consistent lock ordering by UUID comparison prevents deadlocks
        boolean sourceFirst = sourceId.getValue().compareTo(targetId.getValue()) < 0;
        AccountId firstId = sourceFirst ? sourceId : targetId;
        AccountId secondId = sourceFirst ? targetId : sourceId;

        Account first = accountRepository.findByIdForUpdate(firstId);
        Account second = accountRepository.findByIdForUpdate(secondId);

        Account source = firstId.equals(sourceId) ? first : second;
        Account target = firstId.equals(targetId) ? first : second;

        transferDomainService.transfer(source, target, amount);

        accountRepository.save(source);
        accountRepository.save(target);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -Dtest=TransferServiceTest -q
```
Expected: Tests run: 2, Failures: 0, Errors: 0.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/application/service/TransferService.java
git add src/test/java/io/github/zeimerxsw/wallet/application/service/TransferServiceTest.java
git commit -m "feat: add TransferService with deadlock-safe lock ordering + TransferServiceTest"
```

---

### Task 11: Register Use Case + RegisterServiceTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/service/RegisterService.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/application/service/AccountService.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/application/service/RegisterServiceTest.java`

- [ ] **Step 1: Write RegisterServiceTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.model.User;
import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock UserRepository userRepository;
    @Mock AccountRepository accountRepository;
    @Mock PasswordEncoder passwordEncoder;

    RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(userRepository, accountRepository, passwordEncoder);
    }

    @Test
    void register_hashesPasswordBeforeSave() {
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registerService.register(new RegisterCommand("user@example.com", "rawPassword"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed");
        assertThat(userCaptor.getValue().getPasswordHash()).doesNotContain("rawPassword");
    }

    @Test
    void register_createsAccountWithZeroBalance() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registerService.register(new RegisterCommand("user@example.com", "password"));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalance()).isEqualTo(Money.zero());
    }

    @Test
    void register_returnsNonNullUserIdAndAccountId() {
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterResult result = registerService.register(new RegisterCommand("user@example.com", "password"));

        assertThat(result.userId()).isNotNull();
        assertThat(result.accountId()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=RegisterServiceTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `RegisterService` does not exist yet.

- [ ] **Step 3: Write RegisterService.java**

```java
package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.model.User;
import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.in.RegisterUseCase;
import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RegisterService implements RegisterUseCase {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResult register(RegisterCommand command) {
        String hashedPassword = passwordEncoder.encode(command.rawPassword());
        User user = new User(UUID.randomUUID(), command.email(), hashedPassword);
        userRepository.save(user);

        Account account = new Account(AccountId.generate(), Money.zero());
        accountRepository.save(account);

        return new RegisterResult(user.getId(), account.getId().getValue());
    }
}
```

- [ ] **Step 4: Write AccountService.java**

```java
package io.github.zeimerxsw.wallet.application.service;

import io.github.zeimerxsw.wallet.application.model.TransactionDetail;
import io.github.zeimerxsw.wallet.application.port.out.TransactionQueryPort;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import io.github.zeimerxsw.wallet.domain.port.out.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionQueryPort transactionQueryPort;

    public AccountService(AccountRepository accountRepository, TransactionQueryPort transactionQueryPort) {
        this.accountRepository = accountRepository;
        this.transactionQueryPort = transactionQueryPort;
    }

    public UUID createAccount() {
        Account account = new Account(AccountId.generate(), Money.zero());
        accountRepository.save(account);
        return account.getId().getValue();
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        return accountRepository.findById(AccountId.of(id));
    }

    @Transactional(readOnly = true)
    public Page<TransactionDetail> getTransactions(UUID accountId, Pageable pageable) {
        return transactionQueryPort.findByAccountId(accountId, pageable);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./mvnw test -Dtest=RegisterServiceTest -q
```
Expected: Tests run: 3, Failures: 0, Errors: 0.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/application/service/
git add src/test/java/io/github/zeimerxsw/wallet/application/service/RegisterServiceTest.java
git commit -m "feat: add RegisterService, AccountService + RegisterServiceTest"
```

---

### Task 12: JWT Security Infrastructure + JwtTokenProviderTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/infrastructure/security/JwtTokenProvider.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/infrastructure/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/infrastructure/security/UserDetailsServiceImpl.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/infrastructure/security/SecurityConfig.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/infrastructure/security/JwtTokenProviderTest.java`

- [ ] **Step 1: Write JwtTokenProviderTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider =
            new JwtTokenProvider("test-secret-for-unit-tests-32-chars!!", 3600000L);

    @Test
    void generateToken_producesValidToken() {
        String token = provider.generateToken("user@example.com");
        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void isValid_withExpiredToken_returnsFalse() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider("test-secret-for-unit-tests-32-chars!!", 1L);
        String token = shortLived.generateToken("user@example.com");
        Thread.sleep(10);
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void isValid_withGarbageString_returnsFalse() {
        assertThat(provider.isValid("not.a.jwt.token")).isFalse();
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        String token = provider.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThat(provider.isValid(tampered)).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=JwtTokenProviderTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `JwtTokenProvider` does not exist yet.

- [ ] **Step 3: Write JwtTokenProvider.java**

```java
package io.github.zeimerxsw.wallet.infrastructure.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -Dtest=JwtTokenProviderTest -q
```
Expected: Tests run: 4, Failures: 0, Errors: 0.

- [ ] **Step 5: Write UserDetailsServiceImpl.java**

```java
package io.github.zeimerxsw.wallet.infrastructure.security;

import io.github.zeimerxsw.wallet.application.port.out.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(u -> User.builder()
                        .username(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
```

- [ ] **Step 6: Write JwtAuthenticationFilter.java**

```java
package io.github.zeimerxsw.wallet.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtTokenProvider.isValid(token)) {
                String email = jwtTokenProvider.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 7: Write SecurityConfig.java**

```java
package io.github.zeimerxsw.wallet.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

- [ ] **Step 8: Compile**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/infrastructure/ src/test/java/io/github/zeimerxsw/wallet/infrastructure/
git commit -m "feat: add JWT security — JwtTokenProvider, filter, SecurityConfig + JwtTokenProviderTest"
```

---

### Task 13: Web DTOs and Auth Controller + AuthControllerTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/RegisterRequest.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/RegisterResponse.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/LoginRequest.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/LoginResponse.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/AuthController.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/adapter/in/web/AuthControllerTest.java`

- [ ] **Step 1: Write DTOs**

`RegisterRequest.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String rawPassword
) {}
```

`RegisterResponse.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import java.util.UUID;

public record RegisterResponse(UUID userId, UUID accountId) {}
```

`LoginRequest.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
```

`LoginResponse.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

public record LoginResponse(String token) {}
```

- [ ] **Step 2: Write AuthControllerTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.in.RegisterUseCase;
import io.github.zeimerxsw.wallet.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RegisterUseCase registerUseCase;
    @MockBean AuthenticationManager authenticationManager;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    void register_validRequest_returns201WithIds() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(registerUseCase.register(any(RegisterCommand.class)))
                .thenReturn(new RegisterResult(userId, accountId));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"rawPassword\":\"secure123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"rawPassword\":\"secure123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void token_validCredentials_returns200WithToken() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user@example.com", null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateToken("user@example.com")).thenReturn("signed.jwt.token");

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secure123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./mvnw test -Dtest=AuthControllerTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `AuthController` does not exist yet.

- [ ] **Step 4: Write AuthController.java**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.adapter.in.web.dto.LoginRequest;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.LoginResponse;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.RegisterRequest;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.RegisterResponse;
import io.github.zeimerxsw.wallet.application.port.in.RegisterCommand;
import io.github.zeimerxsw.wallet.application.port.in.RegisterResult;
import io.github.zeimerxsw.wallet.application.port.in.RegisterUseCase;
import io.github.zeimerxsw.wallet.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Register and obtain JWT tokens")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(RegisterUseCase registerUseCase, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.registerUseCase = registerUseCase;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user and create a wallet account")
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        RegisterResult result = registerUseCase.register(new RegisterCommand(request.email(), request.rawPassword()));
        return new RegisterResponse(result.userId(), result.accountId());
    }

    @PostMapping("/token")
    @Operation(summary = "Authenticate and receive a JWT token")
    public LoginResponse token(@RequestBody @Valid LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        return new LoginResponse(jwtTokenProvider.generateToken(auth.getName()));
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./mvnw test -Dtest=AuthControllerTest -q
```
Expected: Tests run: 3, Failures: 0, Errors: 0.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/
git add src/test/java/io/github/zeimerxsw/wallet/adapter/in/web/AuthControllerTest.java
git commit -m "feat: add AuthController (register + token) + AuthControllerTest"
```

---

### Task 14: Account and Transfer Controllers + AccountControllerTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/AccountResponse.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/TransactionResponse.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/dto/CreateTransferRequest.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/AccountController.java`
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/TransferController.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/adapter/in/web/AccountControllerTest.java`

- [ ] **Step 1: Write remaining DTOs**

`AccountResponse.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, BigDecimal balance) {}
```

`TransactionResponse.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id, BigDecimal amount, String type, Instant createdAt) {}
```

`CreateTransferRequest.java`:
```java
package io.github.zeimerxsw.wallet.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID targetAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}
```

- [ ] **Step 2: Write AccountControllerTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.application.service.AccountService;
import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.model.Account;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean AccountService accountService;

    @Test
    void createAccount_returns201WithId() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.createAccount()).thenReturn(accountId);

        mockMvc.perform(post("/accounts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(accountId.toString()));
    }

    @Test
    void getAccount_existingAccount_returns200WithBalance() throws Exception {
        UUID accountId = UUID.randomUUID();
        Account account = new Account(AccountId.of(accountId), Money.of("250.00"));
        when(accountService.getAccount(accountId)).thenReturn(account);

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void getAccount_nonExistent_returns404() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.getAccount(accountId))
                .thenThrow(new AccountNotFoundException(AccountId.of(accountId)));

        mockMvc.perform(get("/accounts/{id}", accountId))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./mvnw test -Dtest=AccountControllerTest -q 2>&1 | tail -5
```
Expected: COMPILE ERROR — `AccountController` does not exist yet.

- [ ] **Step 4: Write AccountController.java**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.adapter.in.web.dto.AccountResponse;
import io.github.zeimerxsw.wallet.adapter.in.web.dto.TransactionResponse;
import io.github.zeimerxsw.wallet.application.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Wallet account management")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new zero-balance account")
    public AccountResponse createAccount() {
        UUID id = accountService.createAccount();
        return new AccountResponse(id, BigDecimal.ZERO);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account balance")
    public AccountResponse getAccount(@PathVariable UUID id) {
        var account = accountService.getAccount(id);
        return new AccountResponse(account.getId().getValue(), account.getBalance().getAmount());
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "List transaction history (paginated, default page size 20)")
    public Page<TransactionResponse> getTransactions(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        return accountService.getTransactions(id, pageable)
                .map(t -> new TransactionResponse(t.id(), t.amount(), t.type().name(), t.createdAt()));
    }
}
```

- [ ] **Step 5: Write TransferController.java**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.adapter.in.web.dto.CreateTransferRequest;
import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.application.port.in.TransferUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
@Tag(name = "Transfers", description = "PIX-like transfers between accounts")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferUseCase transferUseCase;

    public TransferController(TransferUseCase transferUseCase) {
        this.transferUseCase = transferUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Transfer funds between two accounts")
    public void transfer(@RequestBody @Valid CreateTransferRequest request) {
        transferUseCase.transfer(new TransferCommand(
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount()));
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
./mvnw test -Dtest=AccountControllerTest -q
```
Expected: Tests run: 3, Failures: 0, Errors: 0.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/
git add src/test/java/io/github/zeimerxsw/wallet/adapter/in/web/AccountControllerTest.java
git commit -m "feat: add AccountController, TransferController + AccountControllerTest"
```

---

### Task 15: GlobalExceptionHandler + ControllerAdviceTest

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/GlobalExceptionHandler.java`
- Test: `src/test/java/io/github/zeimerxsw/wallet/adapter/in/web/ControllerAdviceTest.java`

- [ ] **Step 1: Write ControllerAdviceTest.java (failing test first)**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.application.service.AccountService;
import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
import io.github.zeimerxsw.wallet.domain.model.AccountId;
import io.github.zeimerxsw.wallet.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ControllerAdviceTest {

    @Autowired MockMvc mockMvc;
    @MockBean AccountService accountService;

    @Test
    void accountNotFound_returns404WithCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id))
                .thenThrow(new AccountNotFoundException(AccountId.of(id)));

        mockMvc.perform(get("/accounts/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void insufficientFunds_returns422WithCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id))
                .thenThrow(new InsufficientFundsException(AccountId.of(id), Money.of("10.00"), Money.of("100.00")));

        mockMvc.perform(get("/accounts/{id}", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void illegalArgument_returns400WithCode() throws Exception {
        UUID id = UUID.randomUUID();
        when(accountService.getAccount(id))
                .thenThrow(new IllegalArgumentException("invalid uuid"));

        mockMvc.perform(get("/accounts/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=ControllerAdviceTest -q 2>&1 | tail -10
```
Expected: Tests FAIL — no handler returns the expected `code` JSON field yet.

- [ ] **Step 3: Write GlobalExceptionHandler.java**

```java
package io.github.zeimerxsw.wallet.adapter.in.web;

import io.github.zeimerxsw.wallet.domain.exception.AccountNotFoundException;
import io.github.zeimerxsw.wallet.domain.exception.InsufficientFundsException;
import io.github.zeimerxsw.wallet.domain.exception.SameAccountTransferException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "ACCOUNT_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("code", "INSUFFICIENT_FUNDS", "message", ex.getMessage()));
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ResponseEntity<Map<String, String>> handleSameAccount(SameAccountTransferException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("code", "SAME_ACCOUNT_TRANSFER", "message", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "EMAIL_ALREADY_TAKEN", "message", "Email is already registered"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "VALIDATION_ERROR", "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", "VALIDATION_ERROR", "message", message));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -Dtest=ControllerAdviceTest -q
```
Expected: Tests run: 3, Failures: 0, Errors: 0.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/adapter/in/web/GlobalExceptionHandler.java
git add src/test/java/io/github/zeimerxsw/wallet/adapter/in/web/ControllerAdviceTest.java
git commit -m "feat: add GlobalExceptionHandler (404/422/409/400) + ControllerAdviceTest"
```

---

### Task 16: OpenAPI Documentation

**Files:**
- Create: `src/main/java/io/github/zeimerxsw/wallet/infrastructure/config/OpenApiConfig.java`

- [ ] **Step 1: Write OpenApiConfig.java**

```java
package io.github.zeimerxsw.wallet.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet API")
                        .description("Digital wallet with PIX-like transfers")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS.

After Task 17 (Docker Compose), starting the app will serve Swagger UI at `http://localhost:8080/swagger-ui.html`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/io/github/zeimerxsw/wallet/infrastructure/config/OpenApiConfig.java
git commit -m "feat: add OpenAPI config with JWT bearer security scheme"
```

---

### Task 17: Docker Compose

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Write docker-compose.yml**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: wallet
      POSTGRES_USER: wallet
      POSTGRES_PASSWORD: wallet
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wallet"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

- [ ] **Step 2: Start PostgreSQL and verify**

```bash
docker compose up -d
docker compose ps
```
Expected: `postgres` service status shows `healthy`.

- [ ] **Step 3: Commit**

```bash
git add docker-compose.yml
git commit -m "feat: add Docker Compose with PostgreSQL 16"
```

---

### Task 18: Concurrency Integration Test

**Files:**
- Test: `src/test/java/io/github/zeimerxsw/wallet/integration/ConcurrencyIT.java`
- Modify: `pom.xml` (add surefire config to exclude `integration` tag from default run)

- [ ] **Step 1: Add surefire exclusion to pom.xml**

Inside the `<build><plugins>` block, add:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludedGroups>integration</excludedGroups>
    </configuration>
</plugin>
```

- [ ] **Step 2: Write ConcurrencyIT.java**

```java
package io.github.zeimerxsw.wallet.integration;

import io.github.zeimerxsw.wallet.adapter.out.persistence.AccountJpaEntity;
import io.github.zeimerxsw.wallet.adapter.out.persistence.AccountJpaRepository;
import io.github.zeimerxsw.wallet.adapter.out.persistence.TransactionJpaRepository;
import io.github.zeimerxsw.wallet.application.port.in.TransferCommand;
import io.github.zeimerxsw.wallet.application.port.in.TransferUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class ConcurrencyIT {

    @Autowired TransferUseCase transferUseCase;
    @Autowired AccountJpaRepository accountJpaRepository;
    @Autowired TransactionJpaRepository transactionJpaRepository;

    UUID accountAId;
    UUID accountBId;

    @BeforeEach
    void setUp() {
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        AccountJpaEntity accountA = new AccountJpaEntity();
        accountA.setId(UUID.randomUUID());
        accountA.setBalance(new BigDecimal("1000.00"));
        accountJpaRepository.save(accountA);
        accountAId = accountA.getId();

        AccountJpaEntity accountB = new AccountJpaEntity();
        accountB.setId(UUID.randomUUID());
        accountB.setBalance(new BigDecimal("1000.00"));
        accountJpaRepository.save(accountB);
        accountBId = accountB.getId();
    }

    @Test
    void concurrentTransfers_maintainConsistentTotalBalance() throws Exception {
        int threads = 10;
        BigDecimal transferAmount = new BigDecimal("10.00");
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final boolean aToB = i < 5;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    if (aToB) {
                        transferUseCase.transfer(new TransferCommand(accountAId, accountBId, transferAmount));
                    } else {
                        transferUseCase.transfer(new TransferCommand(accountBId, accountAId, transferAmount));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) f.get();
        executor.shutdown();

        BigDecimal balanceA = accountJpaRepository.findById(accountAId).orElseThrow().getBalance();
        BigDecimal balanceB = accountJpaRepository.findById(accountBId).orElseThrow().getBalance();

        assertThat(balanceA.add(balanceB)).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(transactionJpaRepository.count()).isEqualTo(threads * 2L);
    }
}
```

- [ ] **Step 3: Run full default test suite (excludes integration tag)**

```bash
./mvnw test -q
```
Expected: BUILD SUCCESS. Tests run: 26, Failures: 0, Errors: 0.

- [ ] **Step 4: Run integration test against PostgreSQL (requires `docker compose up -d` first)**

```bash
./mvnw test -Dgroups=integration -q
```
Expected: Tests run: 1, Failures: 0 (verifies pessimistic locking prevents data corruption).

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/test/java/io/github/zeimerxsw/wallet/integration/ConcurrencyIT.java
git commit -m "test: add ConcurrencyIT for pessimistic lock verification + surefire exclusion"
```

---

## Final Verification

- [ ] **Run all non-integration tests and confirm 26 pass**

```bash
./mvnw test -q 2>&1 | grep -E "Tests run:|BUILD"
```
Expected:
```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Expected test breakdown:
| Class | Tests |
|-------|-------|
| `AccountTest` | 5 |
| `TransferServiceTest` | 2 |
| `RegisterServiceTest` | 3 |
| `AccountPersistenceAdapterTest` | 3 |
| `JwtTokenProviderTest` | 4 |
| `AuthControllerTest` | 3 |
| `AccountControllerTest` | 3 |
| `ControllerAdviceTest` | 3 |
| **Total** | **26** |

- [ ] **Final commit**

```bash
git add -A
git commit -m "chore: wallet implementation complete — hexagonal DDD + JWT + OpenAPI"
```
