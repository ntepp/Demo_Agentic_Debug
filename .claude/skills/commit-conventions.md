---
description: Format Conventional Commit messages for this financial service
---

## Format
```
<type>(<scope>): <imperative summary, max 72 chars>

- <bullet: what changed>
- <bullet: why / what it fixes>
- <bullet: test added>

Closes <JIRA-ID>
```

## Types
| Type | When |
|---|---|
| `fix` | Bug fix (NPE, wrong calculation, missing guard) |
| `feat` | New feature or endpoint |
| `test` | Adding or fixing tests only |
| `refactor` | Code change without behaviour change |
| `docs` | Documentation only |

## Scopes (this repo)
| Scope | Classes |
|---|---|
| `reporting` | `BatchReportingService`, `ReportingEngine`, `FinancialAmountCalculator` |
| `api` | `OrderController`, DTOs |
| `domain` | `FinancialOrder`, `Portfolio`, `Currency` |
| `config` | `DataInitializer`, application config |

## Example (TTP-2847)
```
fix(reporting): handle missing settlement currency in FinancialAmountCalculator

- Wrapped portfolio.getCurrency() with Optional.ofNullable() — returns BigDecimal.ONE for legacy portfolios
- Legacy FinCore v1 portfolios (e.g. EMERGING-MKT-C) now processed without NPE
- Added FinancialAmountCalculatorTest#shouldHandleMissingCurrency() regression test
- Added FinancialAmountCalculatorTest#shouldCalculateAmountCorrectly() happy-path test

Closes TTP-2847
```
