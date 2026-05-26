---
description: Trace an exception to its root cause in Java source code
---

For Java exception root-cause analysis:

**Read the stack trace bottom-up** — the bottom frame is the entry point, the top frame is where the exception was thrown.

**For NPE specifically:**
1. Find the top frame: `ClassName.method(File.java:LINE)`
2. Open that file at that line
3. Identify every object dereference on that line: `a.b().c().d()` — any of `a`, `b()`, `c()` could be null
4. Determine which is null by reading the code path that created/passed the object
5. Look for TODO/FIXME/BUG comments on or near the line — they often document known debt
6. Check if there is already a migration ticket referenced in comments

**For this codebase — known patterns:**
- `portfolio.getCurrency()` returns null for FinCore v1 legacy portfolios
- The null is introduced in `DataInitializer` for demo purposes (portfolio EMERGING-MKT-C)
- The fix is always an `Optional.ofNullable()` or an explicit null guard with a fallback
