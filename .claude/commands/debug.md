# /debug — Agentic Debug Workflow

Automated root-cause analysis and fix for any production incident.

## Usage
```
/debug <JIRA-ID>
```
Example: `/debug KAN-1`

---

## Overview

```
PHASE 1 — ANALYSIS      (steps 1–4)  gather context: Jira, Confluence, logs, code
          ⬇
    ┌──────────────────────────────────────┐
    │  PLAN PRESENTED — wait for go/no-go │  ← nothing is changed before this
    └──────────────────────────────────────┘
          ⬇  (approved)
PHASE 2 — EXECUTION     (steps 5–8)  Jira → In Progress, fix, test, commit, comment
          ⬇
    ┌──────────────────────────────────────┐
    │  FUNCTIONAL TEST GATE               │  ← user tests the fix in the running app
    └──────────────────────────────────────┘
          ⬇  (user confirms: tests pass)
PHASE 3 — CLOSE         (step 9)     Jira → Done
```

**Rules:**
- Do not modify any file or ticket before the plan is approved.
- Do not close the ticket before the user confirms functional tests pass.

---

## PHASE 1 — Analysis

### Step 1 — Read the Jira ticket

Call `get_issue($ARGUMENTS)` to fetch the full ticket.

Extract:
- **Error type** (exception class, error message)
- **Affected service / component**
- **Any stack trace or log snippet** in the description
- **Priority and reported impact**

> The ticket drives everything. Do not assume the error type before reading it.

---

### Step 2 — Read the architecture documentation

Search Confluence for documentation related to the **affected component** from Step 1.

Extract:
- Component responsibilities
- Known constraints or migration debt
- Incident history (previous similar failures)

---

### Step 3 — Search the application logs

Search logs for the **exact exception class and/or affected class** found in Steps 1–2.

Capture:
- Full stack trace with **file name and line number**
- Timestamp and frequency (recurring or one-off?)
- Which data triggered the error (order ID, portfolio ID, etc.)
- Surrounding log lines for context

> Every subsequent step is driven by the actual line number from the logs — never by assumptions.

---

### Step 4 — Locate and read the defective code

Open the **exact file and line number** from the stack trace.

Read:
- The failing line and ±40 lines of surrounding context
- Any TODO/FIXME/BUG comments near the defective line
- The method signature and its callers

Identify:
- Which expression is null / what invariant is violated
- Why the condition is possible (design gap, missing guard, etc.)
- Whether this is already a known issue

---

## ⛔ VALIDATION GATE — Present the plan and wait for approval

After Steps 1–4, **stop and present the plan below. Do not proceed until the user approves.**

---

### PLAN: `<JIRA-ID>` — `<ticket title>`

**🔍 Root cause**
> One sentence: what is null/wrong, in which method, and why it can happen.

**📍 Location**
> `ClassName.java:LINE` — `methodName()`

**💥 Impact**
> How many orders/requests are affected, since when, and whether it is recurring.

**🔧 Proposed fix**
> Plain-language description of the code change.

**🧪 Regression test**
> Class and method name that will be created, and what scenario it covers.

**📝 Commit message preview**
```
fix(<scope>): <summary>

- <change 1>
- <regression test added>

Closes <JIRA-ID>
```

---

**Waiting for your approval. Reply `yes` / `go` / `proceed`, or give feedback to adjust.**

---

## PHASE 2 — Execution *(only after approval)*

### Step 5 — Move Jira to In Progress

**First action after approval — before touching any code.**

Call `transition_issue(<JIRA-ID>, "In Progress")`.

This signals that work has started and makes the fix traceable in the audit trail.

---

### Step 6 — Apply the fix

Edit only the defective section. Rules:
- Do not change method signatures (other callers may exist)
- Prefer `Optional` for nullable domain objects
- Add an inline comment referencing the Jira ticket number on the changed line
- Preserve all existing log statements

---

### Step 7 — Generate the regression test

**Derive the test from what was found in Steps 3–4 — not from a template.**

Determine:
- Which class to test (`<FailingClass>Test`)
- Which method to test (the one from the stack trace)
- What input triggers the bug (the exact null/bad value from the logs)
- What the correct behaviour should be after the fix

Create at: `src/test/java/<package>/<FailingClass>Test.java`

The test must:
1. Reproduce the exact failure scenario (regression — `assertDoesNotThrow` or equivalent)
2. Verify the fix produces the expected result (happy path)

Use JUnit 5. Naming: `shouldHandle<MissingThing>()` for the regression, `should<Expected>When<Condition>()` for the happy path.

> Always match the failing class: bug in `RiskCalculatorService` → `RiskCalculatorServiceTest`.

---

### Step 8 — Prepare and present the commit message

Format as Conventional Commit:

```
fix(<scope>): <what was fixed>

- <what changed in production code>
- <regression test: ClassName#methodName()>

Closes <JIRA-ID>
```

Post this commit message ready to copy. Also add a comment on the Jira ticket:
`add_comment(<JIRA-ID>, "Fix applied: <root cause one-liner>. Regression test added: <TestClass>#<method>. Awaiting functional validation.")`

---

## ⏸ FUNCTIONAL TEST GATE — Ask the user to validate in the running app

After Step 8, **stop and present the following testing instructions. Do not close the ticket yet.**

---

### 🧪 Please run functional tests before we close `<JIRA-ID>`

**Start the application** (if not already running):
```bash
# Local (H2, no Docker needed)
$env:JAVA_HOME = "D:\app\jdk-25.0.3"
cd order-service
mvn spring-boot:run

# Or full stack with Docker
./scripts/start-demo.sh
```

**Trigger the scenario that was failing:**
```bash
# Manually trigger one batch cycle
curl -X POST http://localhost:8080/api/orders/batch-report

# Check that previously failing orders/portfolios now succeed
curl http://localhost:8080/api/orders/failed

# Confirm no more errors in logs for the affected class
# (look for the exception that was in the stack trace)
```

**Run the regression test:**
```bash
$env:JAVA_HOME = "D:\app\jdk-25.0.3"
mvn test -Dtest="<FailingClass>Test" -f order-service/pom.xml
```

> Once all checks pass, reply **`done`** / **`tests pass`** / **`ok`** and I will close the Jira ticket.

---

## PHASE 3 — Close *(only after user confirms functional tests pass)*

### Step 9 — Close the Jira ticket

1. `transition_issue(<JIRA-ID>, "Done")` — mark as resolved
2. `add_comment(<JIRA-ID>, "Functional tests confirmed by developer. Ticket closed.")` — final audit trail entry

---

## What makes this workflow adaptive

- Steps 1–3 discover the actual error, class, and line number from live data — never hardcoded
- The **plan gate** ensures no code is changed without explicit approval
- Jira moves to **In Progress immediately** when work starts, not at the end
- Step 7 generates tests specific to the actual failing class
- The **functional test gate** prevents closing a ticket on code alone — the fix must be validated in a running app
- Jira moves to **Done only after human confirmation** — Claude never self-approves closure
