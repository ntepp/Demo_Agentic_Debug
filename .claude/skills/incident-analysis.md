---
description: Correlate Jira ticket, Confluence docs, and logs to build a complete picture of a production incident
---

When performing incident analysis, always triangulate three sources:

**1. Jira ticket** — the reported symptom
- What the user/trader saw (error message, failed operation)
- Which service or component is mentioned
- Timestamp of first report

**2. Confluence** — the intended design
- How the component was designed to work
- Known limitations or migration debt
- Previous similar incidents and their fixes

**3. Application logs** — the actual runtime behaviour
- The exact exception and stack trace
- Which data triggered the failure (portfolio ID, order ID, etc.)
- Whether the failure is isolated or recurring

**Output format:**
```
INCIDENT SUMMARY
────────────────
Ticket:    <ID> — <title>
Component: <service/class>
Symptom:   <what fails from the user's perspective>
Root data: <which entity/value caused the failure>
Since:     <first occurrence timestamp>
Frequency: <every batch cycle / intermittent / one-off>
Blast radius: <N orders affected / batch completely blocked>
Confidence: HIGH / MEDIUM / LOW
```

Always state confidence. LOW = only 1 of 3 sources confirmed the issue.
