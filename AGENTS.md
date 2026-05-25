# meetingandclass — operating notes

## Required environment variables (production)

Set these before starting the app. The fallback values in
`src/main/resources/application.properties` are **dev only**.

| Variable                | Required | Example / Notes                                                            |
| ----------------------- | :------: | -------------------------------------------------------------------------- |
| `JWT_SECRET`            | yes      | Base64-encoded, ≥ 32 bytes. `openssl rand -base64 48`                      |
| `DB_URL`                | yes      | `jdbc:postgresql://localhost:5432/meeting_db`                              |
| `DB_USERNAME`           | yes      | `postgres`                                                                 |
| `DB_PASSWORD`           | yes      | DB password                                                                |
| `MAIL_USERNAME`         | yes (for emails) | Gmail address                                                       |
| `MAIL_PASSWORD`         | yes (for emails) | App-specific Gmail password                                         |
| `MAIL_FROM`             | no       | Default `noreply@cuniv-naama.dz`                                           |
| `MAIL_ADMIN`            | no       | Recipient of meeting-summary PDFs. Default `admin@cuniv-naama.dz`          |
| `CORS_ALLOWED_ORIGINS`  | no       | Comma-separated origins. Default `http://localhost:8080,http://127.0.0.1:8080` |
| `JWT_EXPIRATION`        | no       | Token TTL in ms. Default `3600000` (1 hour).                              |
| `LOGIN_MAX_ATTEMPTS`    | no       | Default `5`.                                                               |
| `LOGIN_LOCK_MINUTES`    | no       | Default `15`.                                                              |
| `UPLOADS_DIR`           | no       | Folder for meeting summary PDFs. Default `uploads`.                        |
| `SUPER_ADMIN_PASSWORD`  | no       | Used **only on first run** when seeding the SUPER_ADMIN. If unset, a random one-time password is logged. The seeder NEVER overwrites an existing admin. |

## Build / verify

```bash
./mvnw -q compile     # compile only
./mvnw -q test        # uses in-memory H2 — no PostgreSQL required
./mvnw spring-boot:run
```

## Frontend dashboard pages — important caveat

Dashboard HTML files (`super-admin-dashboard.html`, `dos-dashboard.html`, etc.)
are static and **publicly downloadable** by anyone. They contain only layout —
no data — and every API call they make is protected by JWT + `@PreAuthorize`.
Never put secrets or privileged data inside the HTML itself. Real authorization
is enforced server-side on `/api/**` endpoints; the client-side
`requireAuth(['ROLE'])` redirect is for UX only.
