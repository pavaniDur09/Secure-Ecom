# Week 9 — Secure E-commerce Platform (Simplified, Beginner-Friendly)

A Spring Boot app implementing JWT auth, role-based access control (RBAC),
method-level security, simplified multi-tenancy, rate limiting, and audit logging —
all written in plain, heavily-commented code so it's easy to follow.

## 0. Running in GitHub Codespaces (recommended, zero local setup)

This repo includes a `.devcontainer/devcontainer.json`, which tells Codespaces to
automatically provision Java 17 + Maven for you - no install steps needed.

1. On GitHub, click **Code → Codespaces → Create codespace on main**
2. Wait for the container to build (first time only, ~1-2 minutes) — you'll see
   "Running postCreateCommand" downloading dependencies
3. Once the terminal is ready, run:
   ```bash
   mvn spring-boot:run
   ```
4. Watch for a popup/notification in the bottom-right: **"Your application running on port 8081 is available"** — click **Open in Browser**, or check the **Ports** tab
5. Test it with curl directly in the Codespaces terminal (see section 2 below)

Anyone who opens this repo in Codespaces gets the exact same environment - they don't
need Java or Maven installed on their own machine at all.

## 1. Run it locally instead

Requires Java 17+ and internet access (Maven needs to download dependencies the first time).

```bash
cd week9-secure-ecommerce
mvn spring-boot:run
```

The app starts on **http://localhost:8081**. On first startup it auto-creates a demo
tenant (`tenant1`) and an admin account:

```
email:    admin@tenant1.com
password: Admin123!
```

You can browse the database at http://localhost:8081/h2-console
(JDBC URL: `jdbc:h2:mem:ecommerce`, user: `sa`, no password).

## 2. Try the API (curl examples)

**Register a customer:**
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@tenant1.com","password":"Pass1234!","tenantId":"tenant1","role":"CUSTOMER"}'
```

**Log in as the seeded admin:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tenant1.com","password":"Admin123!"}'
```
Copy the `accessToken` from the response for the next calls.

**Create a product (needs ADMIN or VENDOR role):**
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"name":"Coffee Mug","description":"Ceramic mug","price":9.99,"stock":50}'
```

**List products in your tenant:**
```bash
curl http://localhost:8081/api/products -H "Authorization: Bearer <accessToken>"
```

**Place an order (needs CUSTOMER or ADMIN):**
```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <customerAccessToken>" \
  -d '{"productId":1,"quantity":2}'
```

**Refresh an expired access token:**
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

**View your own profile (any logged-in user):**
```bash
curl http://localhost:8081/api/users/me -H "Authorization: Bearer <accessToken>"
```

**Update your own profile (name and/or password):**
```bash
curl -X PUT http://localhost:8081/api/users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <accessToken>" \
  -d '{"fullName":"Jane Doe","currentPassword":"Admin123!","newPassword":"NewPass123!"}'
```

**List all users in your tenant (ADMIN only):**
```bash
curl http://localhost:8081/api/users -H "Authorization: Bearer <adminAccessToken>"
```

**Change another user's role (ADMIN only):**
```bash
curl -X PUT http://localhost:8081/api/users/2/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <adminAccessToken>" \
  -d '{"role":"VENDOR"}'
```

**Forgot password — step 1, request a reset token:**
```bash
curl -X POST http://localhost:8081/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tenant1.com"}'
```
The response includes a `demoToken` field (since there's no real email server wired
up — see the warning comment in `PasswordResetService.java`). The same token is also
printed to your console log.

**Forgot password — step 2, use the token to set a new password:**
```bash
curl -X POST http://localhost:8081/api/auth/reset-password/confirm \
  -H "Content-Type: application/json" \
  -d '{"token":"<tokenFromStep1>","newPassword":"BrandNewPass123!"}'
```

Try calling `/api/products` **without** a token — you'll get `401 Unauthorized`.
Try creating a product while logged in as a `CUSTOMER` — you'll get `403 Forbidden`.
That's RBAC + method-level security working.

## 3. How each requirement from the spec is implemented

| Spec requirement | Where it lives | Notes |
|---|---|---|
| JWT authentication + refresh rotation | `security/jwt/JwtTokenProvider.java`, `service/AuthService.java` | Access token (15 min) + refresh token (7 days). Refreshing blacklists the old refresh token and issues a new pair ("rotation"). |
| Token blacklisting / logout | `security/jwt/TokenBlacklistService.java` | In-memory `Set` for the demo. **Swap for Redis in production** so it works across server restarts/instances. |
| Role-based access control | `model/enums/UserRole.java`, `security/config/SecurityConfig.java` | URL-level rules (`authorizeHttpRequests`) for ADMIN / VENDOR / CUSTOMER. |
| Method-level security | `service/ProductService.java`, `service/OrderService.java`, `service/TenantService.java` | `@PreAuthorize` annotations, enabled via `@EnableMethodSecurity` in `SecurityConfig`. |
| Custom security expressions | `service/TenantSecurityService.java` | Called from `@PreAuthorize("@tenantSecurityService.hasAccessToProduct(#productId)")`. |
| Permission-based fine-grained authorization | `model/enums/Permission.java`, `security/authorization/RolePermissions.java`, `service/UserService.java` | Each role maps to a set of permissions. `User`/`JwtAuthenticationFilter` grant both `ROLE_*` and `PERMISSION_*` authorities, so `@PreAuthorize` can check either. See it in action on `/api/users/**`. |
| User profile management | `controller/UserController.java`, `service/UserService.java` | `GET/PUT /api/users/me` (any user, own profile only), `GET /api/users` and `PUT /api/users/{id}/roles` (admin only). |
| Password reset with email verification | `controller/AuthController.java`, `service/PasswordResetService.java`, `PasswordResetToken` entity | Full two-step flow with a 30-minute expiring, single-use token. No real email server is wired up, so the token is logged to the console and returned in the API response for local testing — clearly marked DEMO ONLY, since a real deployment must never return the token in the response. |
| Multi-tenant architecture | `tenantId` column on `User`/`Product`/`Order` | **Simplified on purpose** — one shared database with a `tenantId` column ("row-level multi-tenancy") instead of a separate database per tenant. This is what most real SaaS products start with; see the note in `Tenant.java` for how to upgrade to full per-tenant database routing later. |
| API rate limiting | `security/jwt/RateLimitFilter.java` | Simple in-memory sliding window on `/api/auth/login`, to block brute-force attacks. The spec suggests Resilience4j — this hand-rolled version teaches the same idea with no extra library. |
| Security headers + CORS | `security/config/SecurityConfig.java` | HSTS, CSP, referrer policy, frame options, and a CORS policy. |
| Security event auditing | `security/audit/AuditService.java` | Logs structured events (`AUTH_SUCCESS`, `AUTH_FAILED`, `PRODUCT_CREATED`, etc.) to the console/log file. |
| Password policy + account lockout | `service/AuthService.java`, `User.java` | BCrypt (strength 12) hashing; account locks after 5 failed login attempts. |
| Input validation | `model/dto/*Request.java` | `@NotBlank`, `@Email`, `@Size`, `@Positive` — caught centrally by `exception/GlobalExceptionHandler.java`. |
| OAuth2 social login (Google/GitHub) | **Not implemented in this simplified version** | Full OAuth2 login needs real client IDs/secrets registered with Google/GitHub, which only you can create. See "Adding OAuth2" below for how to wire it in once you have credentials. |
| Security testing (OWASP ZAP) | **Manual step, outside the code** | Run the app, then point OWASP ZAP's automated scanner at `http://localhost:8081` — no code changes needed for that part of the assignment. |

## 4. What's intentionally simplified (and how to extend it)

This version favors **clarity over completeness** so the code is easy to read as a
learning project. A few things were simplified from the original spec — each is
called out with a comment in the code, plus how to level it up:

- **Multi-tenancy**: single shared DB + `tenantId` column, instead of routing to a
  separate database per tenant. To go further, look up Spring's
  `AbstractRoutingDataSource` (the original spec's sample code shows the shape of it).
- **Rate limiting**: hand-rolled in-memory counter instead of Resilience4j. Add the
  `resilience4j-spring-boot3` starter and replace `RateLimitFilter` with a
  `@RateLimiter`-annotated method once you're comfortable with the basic idea.
- **Token blacklist**: in-memory `Set` instead of Redis. Add
  `spring-boot-starter-data-redis` and swap the `Set` for `RedisTemplate` calls.
- **OAuth2 social login**: not wired up (needs your own Google/GitHub app
  credentials). Add `spring-boot-starter-oauth2-client`, register your app with
  Google/GitHub to get a client ID/secret, then add an `oauth2Login(...)` block to
  `SecurityConfig` — the original spec's sample code shows the shape to aim for.
- **Two-factor authentication**: marked optional in the spec — not implemented here.

## 5. Project structure

```
week9-secure-ecommerce/
├── pom.xml
├── docker-compose.yml          (optional — for Postgres later; H2 works out of the box)
├── src/main/java/com/ecommerce/
│   ├── EcommerceApplication.java
│   ├── config/
│   │   └── DataSeeder.java             # creates demo tenant + admin on startup
│   ├── security/
│   │   ├── config/SecurityConfig.java  # the heart of the security setup
│   │   ├── jwt/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── TokenBlacklistService.java
│   │   │   └── RateLimitFilter.java
│   │   └── audit/AuditService.java
│   ├── model/
│   │   ├── entity/ (User, Tenant, Product, Order)
│   │   ├── enums/UserRole.java
│   │   └── dto/ (LoginRequest, RegisterRequest, JwtResponse, ...)
│   ├── repository/ (User, Product, Order, Tenant)
│   ├── service/ (AuthService, ProductService, OrderService, TenantService,
│   │             TenantSecurityService, UserDetailsServiceImpl)
│   ├── controller/ (Auth, Product, Order, Tenant)
│   └── exception/GlobalExceptionHandler.java
└── src/test/java/com/ecommerce/
    ├── security/JwtTokenTest.java
    └── controller/SecurityIntegrationTest.java
```

## 6. Suggested learning path through the code

1. `EcommerceApplication.java` — the entry point.
2. `security/config/SecurityConfig.java` — read this first, it's the map of the
   whole security setup.
3. `security/jwt/JwtTokenProvider.java` — how tokens are made and checked.
4. `security/jwt/JwtAuthenticationFilter.java` — how a token on a request becomes
   "this user is logged in" inside Spring.
5. `service/AuthService.java` — register/login/refresh/logout logic.
6. `service/ProductService.java` — see `@PreAuthorize` in action for fine-grained
   permission checks.
7. Run it, hit the endpoints with curl, and watch the audit log lines print in your
   console as you go.
