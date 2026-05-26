---
description: Generate JUnit 5 tests derived from the actual bug found — not from a fixed template
---

## Process

1. **Identify the failing class and method** from the stack trace (e.g. `FinancialAmountCalculator.calculateAmount`)
2. **Identify the triggering input** from the logs (e.g. `portfolio.getCurrency() == null`)
3. **Build the minimal test object graph** that reproduces the failure
4. **Assert the corrected behaviour** after the fix

## Test structure
```java
@Test
void shouldHandle<MissingThing>() {
    // Arrange — reproduce the exact condition from the logs
    <Entity> entity = <Entity>.builder()
            .<problematicField>(null)   // ← the root cause
            .build();

    // Act — call the method that was in the stack trace
    // Assert — must NOT throw after the fix
    assertDoesNotThrow(() -> serviceUnderTest.method(entity));
}

@Test
void should<ExpectedBehavior>When<Condition>() {
    // Arrange — happy path with all fields configured
    // Act
    // Assert — expected return value
}
```

## Naming conventions
- Regression test: `shouldHandle<MissingField>()` or `shouldNotThrowWhen<Condition>()`
- Happy path: `should<ExpectedResult>When<GoodInput>()`
- Validation test: `shouldThrowWhen<Invalid>()`

## File location
`src/test/java/<same package as class under test>/<ClassName>Test.java`

## Test class annotation
Pure unit tests (no Spring context): no annotation needed, just `class XTest { }`.
Integration tests: `@SpringBootTest`.
