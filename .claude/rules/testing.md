# Testing Rules
- Test class naming: `[Controller Name]MvcTest.java`.
- Use `@WebMvcTest` for controller tests; mock all dependencies with `@MockitoBean`.
- Use the Jackson 3 ObjectMapper for JSON serialization/deserialization in tests.
- Follow the AAA pattern, commenting each section:
  ```
  // Arrange
  // Act
  // Assert
  ```
- Test names: `should_[expected]_when_[condition]`.
- One assertion block per test.
- Never test private methods.
