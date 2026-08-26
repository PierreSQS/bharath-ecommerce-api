# API Design Rules
- All endpoints: `/api/v1/resource`.

## HTTP Methods & Status Codes
| Operation | Verb   | Success Response                  |
|-----------|--------|-----------------------------------|
| Create    | POST   | `201 Created` with body           |
| Retrieve  | GET    | `200 OK`                          |
| Update    | PUT    | `200 OK` with updated body        |
| Delete    | DELETE | `204 No Content`                  |

## Error Responses
- Validation errors → `400 Bad Request`, listing all field errors.
- Business rule violations → `422 Unprocessable Entity` (not 400).
- Duplicate resource → `409 Conflict`.

## 400 vs 422
The two are decided by *where* the rule is enforced, not by how invalid the input feels.

| | `400 Bad Request` | `422 Unprocessable Content` |
|---|---|---|
| Meaning | The request is malformed — it cannot be interpreted. | The request is well-formed but violates a domain rule. |
| Enforced by | Bean Validation annotations on the DTO. | An explicit check in the **service** layer. |
| Raised as | `MethodArgumentNotValidException` | `BusinessRuleException` |
| Examples | Missing required field, blank string, wrong type, oversized string. | Value outside an allowed domain range, illegal state transition, insufficient stock. |

Rules:
- Use DTO annotations (`@NotNull`, `@NotBlank`, `@Size`) only for presence, format, and type.
- Enforce domain value rules in the service and throw `BusinessRuleException`.
- Never put a range annotation (`@Min`, `@Max`) on a DTO field whose range is meant to yield 422. Bean Validation runs first and short-circuits with 400, which makes the service-layer check unreachable dead code.
- A field can legitimately carry both: `@NotNull` on the DTO (400 when absent) plus a service-layer range check (422 when present but out of range).
