---
description: Apply a null-safety fix without breaking existing callers or batch behaviour
---

## Rules for this codebase

1. **Do not change method signatures** — `calculateAmount(FinancialOrder)` is called by `ReportingEngine` which is called by `BatchReportingService`
2. **Do not remove log statements** — log search during the demo depends on specific strings like `"Batch reporting failed"` and `"Currency is null"`
3. **Keep the batch loop intact** — each order is already wrapped in try/catch in `ReportingEngine`; do not flatten that structure
4. **Add a `// Fixed: TTP-XXXX` comment** on the changed line for traceability
5. **Do not introduce new dependencies** — use `java.util.Optional` (already in JDK)

## Minimal safe change pattern

Replace a chained null-unsafe call:
```java
// Before — NPE when getCurrency() is null
BigDecimal x = portfolio.getCurrency().getAmount();

// After — null safe, backward compatible
BigDecimal x = Optional.ofNullable(portfolio.getCurrency())
        .map(Currency::getAmount)
        .orElse(BigDecimal.ONE);   // Fixed: TTP-2847
```

Add import at top of file: `import java.util.Optional;`
