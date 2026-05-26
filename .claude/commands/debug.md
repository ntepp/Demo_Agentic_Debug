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
PHASE 1 — ANALYSIS   (steps 1–4)   read Jira, Confluence, logs, code
          ⬇
    ════════════════════════════════════════
      HARD STOP — output plan, end turn
      wait for user message to continue
    ════════════════════════════════════════
          ⬇  user replies: go / yes / proceed
PHASE 2 — EXECUTION  (steps 5–8)   Jira → In Progress, fix, test, commit
          ⬇
    ════════════════════════════════════════
      HARD STOP — output test instructions
      wait for user message to continue
    ════════════════════════════════════════
          ⬇  user replies: done / tests pass / ok
PHASE 3 — CLOSE      (step 9)      Jira → Done
```

---

## PHASE 1 — Analysis

Run all four steps without modifying anything. No file edits, no tool writes, no Jira transitions.

### Step 1 — Read the Jira ticket

Call `get_issue($ARGUMENTS)`.

Extract: error type, affected component, stack trace if present, priority, impact.

### Step 2 — Read the architecture documentation

Search Confluence for the affected component. Extract: responsibilities, known constraints, legacy debt, incident history.

### Step 3 — Search the application logs

Search for the exception class and affected class from Steps 1–2.

Capture: full stack trace with **exact file name and line number**, frequency, which data triggered the error, surrounding context.

### Step 4 — Locate and read the defective code

Open the exact file and line from the stack trace. Read ±40 lines of context plus TODO/FIXME comments.

Identify: what is null/wrong, why it can happen, whether it is a known issue.

---

## ════ HARD STOP — VALIDATION GATE ════

**After Step 4: output the plan below, then END YOUR TURN.**
**Do not call any tool (Edit, Write, Bash, MCP) after this block.**
**Wait for the user's next message before doing anything.**

---

## 📋 PLAN — `<JIRA-ID>` · `<ticket title>`

| | |
|---|---|
| 🔍 **Root cause** | _one sentence: what is wrong, where, and why_ |
| 📍 **Location** | `ClassName.java:LINE` — `methodName()` |
| 💥 **Impact** | _N orders/portfolios affected, since when, recurring?_ |
| 🔧 **Fix** | _plain-language description of the code change_ |
| 🧪 **Test** | `<FailingClass>Test#shouldHandle<X>()` |
| 📝 **Commit** | `fix(<scope>): <summary> — Closes <JIRA-ID>` |
| ⚡ **Jira** | current status → **In Progress** (first action after approval) |

---

> ⏸ **Waiting for your approval.**
> Reply **`go`** to proceed with implementation, or describe what to adjust.

_(End of turn — do not continue until the user replies.)_

---

## PHASE 2 — Execution *(starts only when user replies go / yes / proceed)*

### Step 5 — Jira → In Progress

**This is the very first action. Before touching any code.**

`transition_issue(<JIRA-ID>, "In Progress")`

### Step 6 — Apply the fix

Edit only the defective line/section.
- Do not change method signatures
- Prefer `Optional` for nullable objects
- Add inline comment: `// Fixed: <JIRA-ID>`
- Keep all existing log statements

### Step 7 — Generate the regression test

Derive from what was found — never from a fixed template.

- Class: `<FailingClass>Test` (always matches the class in the stack trace)
- Method 1: `shouldHandle<MissingThing>()` — reproduces the exact failure
- Method 2: `should<Expected>When<Condition>()` — happy path after fix
- Path: `src/test/java/<same package>/<FailingClass>Test.java`

### Step 8 — Commit message + Jira comment

Present the ready-to-use commit message:

```
fix(<scope>): <what was fixed>

- <production code change>
- Added <FailingClass>Test#shouldHandle<X>() regression test

Closes <JIRA-ID>
```

Post on the ticket: `add_comment(<JIRA-ID>, "Fix applied: <root cause>. Test added: <TestClass>#<method>. Pending functional validation.")`

---

## ════ HARD STOP — FUNCTIONAL TEST GATE ════

**After Step 8: output the instructions below, then END YOUR TURN.**
**Do not call transition_issue or any other tool after this block.**
**Wait for the user's next message before closing the ticket.**

---

## 🧪 Please validate the fix before we close `<JIRA-ID>`

**1. Start the app (if not running):**
```powershell
$env:JAVA_HOME = "D:\app\jdk-25.0.3"
cd order-service
mvn spring-boot:run
```
Or full stack: `./scripts/start-demo.sh`

**2. Trigger the failing scenario:**
```bash
# Trigger one batch cycle
curl -X POST http://localhost:8080/api/orders/batch-report

# Check the previously failing orders/portfolios now succeed
curl http://localhost:8080/api/orders/failed
```

**3. Run the regression test:**
```powershell
$env:JAVA_HOME = "D:\app\jdk-25.0.3"
mvn test -Dtest="<FailingClass>Test" -f order-service/pom.xml
```

> ⏸ **Reply `done` / `tests pass` / `ok` when all checks are green.**
> I will then close the Jira ticket.

_(End of turn — do not close the ticket until the user replies.)_

---

## PHASE 3 — Close *(starts only when user replies done / tests pass / ok)*

### Step 9 — Jira → Done

1. `transition_issue(<JIRA-ID>, "Done")`
2. `add_comment(<JIRA-ID>, "Functional tests confirmed. Ticket closed.")`

---

## Key rules summary

| Rule | Detail |
|---|---|
| No tool calls before plan approval | Phase 1 is read-only |
| Jira → In Progress is the FIRST action | Before any code change |
| Gate = end of turn | Not a soft suggestion — Claude stops and waits |
| Tests must be class-specific | Always derived from the stack trace, never hardcoded |
| Jira → Done requires human confirmation | Claude never self-approves closure |
