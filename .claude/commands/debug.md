# /debug — Agentic Debug Workflow

Automated root-cause analysis and fix for any production incident.

## Usage
```
/debug <JIRA-ID>
```
Example: `/debug KAN-1`

---

## Overview

The workflow has **two phases** separated by a mandatory validation gate:

```
PHASE 1 — ANALYSIS   (steps 1–4) : gather all context from Jira, Confluence, logs, code
         ⬇
    ┌─────────────────────────────────────┐
    │   PLAN PRESENTED — WAIT FOR GO/NO  │  ← human must approve before anything is changed
    └─────────────────────────────────────┘
         ⬇ (approved)
PHASE 2 — EXECUTION  (steps 5–8) : fix, test, commit, update Jira
```

**Do not modify any file, run any command, or update any ticket before the plan is approved.**

---

## PHASE 1 — Analysis

### Step 1 — Read the Jira ticket

Call `get_issue($ARGUMENTS)` to fetch the full ticket.

Extract and note:
- **Error type** (exception class, error message)
- **Affected service / component**
- **Any stack trace or log snippet** in the description
- **Priority and reported impact**

> The ticket drives everything that follows. Do not assume the error type before reading it.

---

### Step 2 — Read the architecture documentation

Search Confluence for documentation related to the **affected component** from Step 1.

Extract:
- Component responsibilities
- Known constraints or limitations
- Legacy system references or migration debt
- Incident history (any previous similar failures)

---

### Step 3 — Search the application logs

Search logs for the **exact exception class and/or affected class** found in Steps 1–2.

Capture:
- Full stack trace with **file name and line number**
- Timestamp and frequency (recurring or one-off?)
- Which data triggered the error (order ID, portfolio ID, user, etc.)
- Surrounding log lines for context (what happened just before the failure)

> Every subsequent step is driven by the actual line number from the logs — never by assumptions.

---

### Step 4 — Locate and read the defective code

Open the **exact file and line number** from the stack trace.

Read:
- The failing line and ±40 lines of surrounding context
- Any TODO/FIXME/BUG comments near the defective code
- The method signature and its callers

Identify:
- Which expression is null / what invariant is violated
- Why the condition is possible (design gap, missing guard, etc.)
- Whether this is already a known issue (referenced in comments or previous incidents)

---

## ⛔ VALIDATION GATE — Present the plan and wait for approval

After completing Steps 1–4, **stop and present the following plan**. Do not proceed until the user explicitly approves.

---

### PLAN: `<JIRA-ID>` — `<ticket title>`

**🔍 Root cause**
> One sentence: what is null/wrong, in which method, and why it can happen.

**📍 Location**
> `ClassName.java:LINE` — `methodName()`

**💥 Impact**
> How many orders/requests are affected, since when, and whether it is recurring.

**🔧 Proposed fix**
> Plain-language description of the code change (e.g. "Wrap `portfolio.getCurrency()` with `Optional.ofNullable()`, fall back to `BigDecimal.ONE` for legacy portfolios").

**🧪 Regression test**
> Class and method name that will be created, and what scenario it covers.

**📝 Commit message preview**
```
fix(<scope>): <summary>

- <change 1>
- <regression test added>

Closes <JIRA-ID>
```

**⚡ Jira transition**
> `<current status>` → `In Progress`

---

**Waiting for your approval to proceed with implementation.**
Reply `yes` / `go` / `proceed` to execute, or provide feedback to adjust the plan.

---

## PHASE 2 — Execution *(only after approval)*

### Step 5 — Apply the fix

Edit only the defective section. Rules:
- Do not change method signatures (other callers may exist)
- Prefer `Optional` for nullable domain objects
- Add an inline comment referencing the Jira ticket number on the changed line
- Preserve all existing log statements

---

### Step 6 — Generate the regression test

**Derive the test from what was found in Steps 3–4 — not from a template.**

Determine:
- Which class to test (`<FailingClass>Test`)
- Which method to test (the one from the stack trace)
- What input triggers the bug (the exact null/bad value from the logs)
- What the correct behaviour should be after the fix

Create the test file at:
`src/test/java/<package>/<FailingClass>Test.java`

The test must:
1. Reproduce the exact failure scenario (regression test — `assertDoesNotThrow` or equivalent)
2. Verify the fix produces the expected result (happy path)

Use JUnit 5. Naming: `shouldHandle<MissingThing>()` for the regression, `should<Expected>When<Condition>()` for the happy path.

> If the error is in `ReportingEngine`, create `ReportingEngineTest`. If it is in `OrderController`, create `OrderControllerTest`. Always match the failing class.

---

### Step 7 — Prepare the commit message

Format as Conventional Commit derived from the actual changes:

```
fix(<scope>): <what was fixed>

- <what changed in production code>
- <regression test: ClassName#methodName()>

Closes <JIRA-ID>
```

---

### Step 8 — Update the Jira ticket

1. `transition_issue(<JIRA-ID>, "In Progress")` — move the ticket status
2. `add_comment(<JIRA-ID>, ...)` — post a comment summarising root cause, fix applied, and test added

---

## What makes this workflow adaptive

- Steps 1–3 discover the actual error, class, and line number from live data
- Step 4 reads the real code at the real location
- The **plan is always shown and approved** before any file is touched
- Step 6 generates tests specific to what was found — if the bug is in `BatchReportingService`, it creates `BatchReportingServiceTest`, not a hardcoded class name
- The commit message and Jira comment reflect the actual change made
