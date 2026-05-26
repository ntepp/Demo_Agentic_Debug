# /debug — Agentic Debug Workflow

Automated root-cause analysis and fix for any production incident.

## Usage
```
/debug <JIRA-ID>
```
Example: `/debug TTP-2847`

---

## Workflow

### Step 1 — Read the Jira ticket

Use `get_issue($ARGUMENTS)` to fetch the full ticket.

Extract and note:
- **Error type** (exception class, error message)
- **Affected service / component**
- **Any stack trace or log snippet** in the description
- **Priority and reported impact**

> The ticket drives everything that follows. Do not assume the class or error type before reading it.

---

### Step 2 — Read the architecture documentation

Search Confluence for documentation related to the **affected component** identified in Step 1.

Use search terms derived from the ticket (e.g. the service name, the domain entity, the batch job name).

Extract:
- Component responsibilities
- Known constraints or limitations
- Any mention of legacy systems or migration debt
- Incident history section (if present)

---

### Step 3 — Search the application logs

Search logs for the **exact exception class and/or the affected class** found in Steps 1–2.

Capture:
- The full stack trace (class, method, **line number**)
- Timestamp and frequency (is it recurring?)
- Which data triggered the error (order ID, portfolio ID, etc.)
- Surrounding log lines for context (what happened just before the failure)

> The stack trace determines where to look in the code. Every subsequent step is driven by the actual line number from the logs — not by assumptions.

---

### Step 4 — Locate and read the defective code

Open the **exact file and line number** identified in the stack trace.

Read:
- The failing line itself
- ±40 lines of surrounding context
- Any TODO/FIXME/BUG comments near the defective code
- The method signature and its callers

Identify:
- Which expression is null / what invariant is violated
- Why the null/error condition is possible (design gap, missing guard, etc.)
- Whether this is a known issue (TODO comment, previous incident reference)

---

### Step 5 — State the root cause and propose the fix

Present a concise diagnostic before making any change:

```
Root cause: <one sentence — what is null/wrong and why>
Failing class: <ClassName.java:LINE>
Impact: <N orders/requests affected, since when>
Fix: <describe the code change in plain language>
Regression test: <describe what test will verify the fix>
```

Wait for confirmation if this is a live demo. Otherwise proceed.

---

### Step 6 — Apply the fix

Edit only the defective section. Rules:
- Do not change method signatures (other callers may exist)
- Prefer `Optional` for nullable domain objects
- Add a comment referencing the Jira ticket number on the changed line
- Preserve all existing log statements

---

### Step 7 — Generate the regression test

**Derive the test from what you found in Steps 3–4, not from a template.**

Determine:
- Which class to test (`<FailingClass>Test`)
- Which method to test (the one from the stack trace)
- What input triggers the bug (the exact null/bad value from the logs)
- What the correct behavior should be after the fix

Create the test file at the correct path:
`src/test/java/<package>/<FailingClass>Test.java`

The test must:
1. Reproduce the exact failure scenario from the logs (the regression test)
2. Verify the fix produces the expected result (the happy-path assertion)

Use JUnit 5. Method names: `shouldHandleMissing<X>()` for the regression, `shouldCalculate<Y>Correctly()` for the happy path.

---

### Step 8 — Prepare the commit message

Format as Conventional Commit. Derive scope and body from the actual changes made:

```
fix(<scope>): <what was fixed>

- <change 1>
- <change 2 — regression test>

Closes <JIRA-ID>
```

---

### Step 9 — Update the Jira ticket

1. `transition_issue(<JIRA-ID>, "In Progress")` — move status
2. `add_comment(<JIRA-ID>, <summary of root cause and fix>)` — leave audit trail

---

## What makes this workflow adaptive

- Steps 1–3 discover the actual error, class, and line number from live data
- Step 4 reads the real code at the real location
- Step 7 generates tests **specific to what was found** — not a pre-written template
- If the error is in `BatchReportingService`, `OrderController`, or any other class, the workflow produces a `BatchReportingServiceTest`, `OrderControllerTest`, etc.
- The commit message reflects the actual change, not a generic placeholder
