---
description: Java patterns for null safety and correct arithmetic in financial services
---

## Null safety for nullable domain objects

**Preferred — Optional chain:**
```java
BigDecimal multiplier = Optional.ofNullable(portfolio.getCurrency())
        .map(Currency::getAmount)
        .orElse(BigDecimal.ONE);   // neutral multiplier for portfolios without currency
```

**Acceptable — explicit guard with business log:**
```java
Currency currency = portfolio.getCurrency();
if (currency == null) {
    log.warn("Portfolio {} has no settlement currency — using neutral multiplier (TTP-2841)", portfolio.getId());
    return BigDecimal.ONE;
}
```

## BigDecimal rules
- Always specify `RoundingMode` — never use plain `divide()` without a scale
- Use `HALF_UP` for monetary amounts
- Intermediate scale: 4 decimal places; final output: 2

## Batch processing rule
A single order failure must **never** abort the batch.
Each order must be wrapped in try/catch; failed orders get `OrderStatus.FAILED` and the batch continues.

## Import to add when using Optional
```java
import java.util.Optional;
```
