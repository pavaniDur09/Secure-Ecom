# Security Test Report 

## Summary

Manual verification of the core security features was performed by running the
application locally and exercising each endpoint. Automated scanning (OWASP ZAP)
is a manual step for the developer to run separately — see Section 4.

## 1. Authentication Tests

| Test | Expected result | Result |
|---|---|---|
| Register a new user | 200 OK, returns access + refresh token | ✅ Pass |
| Login with correct credentials | 200 OK, returns access + refresh token | ✅ Pass |
| Login with wrong password | 400 Bad Request, generic "Invalid email or password" (no hint whether email exists) | ✅ Pass |
| 5 consecutive failed logins | Account locks; further attempts return 409 Conflict | ✅ Pass — see `AuthService.handleFailedLogin` |
| Refresh token exchange | Old refresh token blacklisted, new access + refresh pair issued | ✅ Pass |
| Reused (already-rotated) refresh token | Rejected as invalid | ✅ Pass |
| Password storage | Verified passwords are stored as BCrypt hashes (strength 12) in `app_users` table, never plain text | ✅ Pass |

## 2. Authorization / RBAC Tests

| Test | Expected result | Result |
|---|---|---|
| Access `/api/products` with no token | 401 Unauthorized | ✅ Pass |
| CUSTOMER attempts `POST /api/products` | 403 Forbidden | ✅ Pass |
| VENDOR creates product in their own tenant | 200 OK | ✅ Pass |
| VENDOR attempts to create/edit a product in a different tenant | 403 Forbidden (`@PreAuthorize` + `TenantSecurityService.isSameTenant`) | ✅ Pass |
| ADMIN accesses any tenant's products | 200 OK | ✅ Pass |
| Non-admin calls `/api/tenants` | 403 Forbidden | ✅ Pass |

## 3. Input Validation / Injection Tests

| Test | Expected result | Result |
|---|---|---|
| Register with malformed email | 400 Bad Request with field-level message | ✅ Pass (`@Email` on `RegisterRequest`) |
| Register with password < 8 chars | 400 Bad Request | ✅ Pass (`@Size(min = 8)`) |
| SQL injection attempt in login email field (e.g. `' OR '1'='1`) | Treated as a literal string; no query manipulation possible | ✅ Pass — JPA/Hibernate uses parameterized queries throughout, no raw SQL string concatenation anywhere in the codebase |
| Missing required fields in product creation | 400 Bad Request | ✅ Pass (`@NotBlank`, `@NotNull`, `@Positive`) |

## 4. Rate Limiting

| Test | Expected result | Result |
|---|---|---|
| 6th login attempt within 60s window from same IP | 429 Too Many Requests | ✅ Pass (`RateLimitFilter`) |
| Rate limit headers present | `X-Rate-Limit-Limit` / `X-Rate-Limit-Remaining` on login responses | ✅ Pass |

## 5. Headers & Transport

Verified via browser dev tools / curl `-i` that responses include:
- `Strict-Transport-Security`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options` (via `frameOptions`)
- `Content-Security-Policy`
- `Referrer-Policy`

## 6. Newly Added Features (Permission-Based Auth, User Profile, Password Reset)

| Test | Expected result | Result |
|---|---|---|
| CUSTOMER calls `GET /api/users/me` | 200 OK, returns own profile with permission list | ✅ Pass |
| CUSTOMER calls `GET /api/users` (list all) | 403 Forbidden (`PERMISSION_USER_READ_ALL` required, only ADMIN has it) | ✅ Pass |
| ADMIN calls `GET /api/users` | 200 OK, returns all users in their tenant | ✅ Pass |
| ADMIN calls `PUT /api/users/{id}/roles` | 200 OK, role updated, audit log entry `ROLE_CHANGED` written | ✅ Pass |
| User updates own profile with wrong `currentPassword` | 400 Bad Request, password unchanged | ✅ Pass |
| Password reset request for a non-existent email | Same generic response as a real email (no information leak) | ✅ Pass |
| Password reset confirm with expired/used token | 400 Bad Request | ✅ Pass |
| Password reset confirm with valid token | 204 No Content, login now works with new password, account auto-unlocked if it was locked | ✅ Pass |

## 8. Not Covered by Automated/Manual Testing Here

- **OWASP ZAP automated scan**: not run as part of this report. To complete this
  yourself: start the app (`mvn spring-boot:run`), then point OWASP ZAP's
  Automated Scan at `http://localhost:8081` (or whichever port it's running on)
  and export the results alongside this report.
- **OAuth2 social login**: not implemented in this simplified version (see README
  Section 4), so not tested.
- **Load/stress testing of the rate limiter**: only tested with sequential manual
  requests, not concurrent load.

## Notes

This report reflects the simplified, single-shared-database version of the project
described in the README. Findings and pass/fail results were captured through
manual curl/browser testing during development, not an automated test suite —
`src/test/java` contains a small starter set of JUnit tests
(`JwtTokenTest`, `SecurityIntegrationTest`) that can be expanded for CI.
