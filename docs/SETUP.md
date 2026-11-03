# Local setup

## Prerequisites

- JDK 21; set `JAVA_HOME` to the JDK directory and put its `bin` on PATH.
- MySQL Server 8 running locally, plus a MySQL client.
- Internet for the Maven wrapper's first run. A separate Maven installation is unnecessary.

A workspace-local JDK 21 was downloaded for verification into `.tools/jdk-21.0.13+9`. On this computer you can use `$env:JAVA_HOME = 'C:\Users\nihis\TraceX\.tools\jdk-21.0.13+9'`. This ignored tools directory is not part of the deliverable source; other teammates should install their own JDK 21.

Spring Boot is pinned to 3.5.16, the public 3.5 patch listed in the [official documentation](https://docs.spring.io/spring-boot/3.5/documentation.html).

## Database

Connect with `mysql -u root -p` and execute:

```sql
CREATE DATABASE traceroute_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'traceroute_app'@'localhost' IDENTIFIED BY '<choose-a-local-password>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON traceroute_db.* TO 'traceroute_app'@'localhost';
```

Replace the password placeholder yourself. Do not commit credentials. The development account has DDL permissions because Hibernate uses `ddl-auto=update`. Use managed migrations and reduced runtime privileges for a production deployment.

`database/schema.sql` is reference documentation only. Do not run it as a second application schema. Spring SQL initialization is explicitly disabled. Initially Hibernate creates only `users`; teammate 2's entities will create their tables when integrated.

## Run (PowerShell)

```powershell
cd backend
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:DB_USER = 'traceroute_app'
$secret = Read-Host 'Database password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
.\mvnw.cmd spring-boot:run
```

The password is provided to the process through its environment, without echoing it or putting it in shell history. `.env` files are ignored by Git but are not loaded automatically by Spring Boot. Optional `DB_URL` overrides the default `jdbc:mysql://localhost:3306/traceroute_db`.

Backend binds to `127.0.0.1:8080` by default. Keep that binding for HTTP Basic over HTTP. Remote deployments require HTTPS and a deliberate server/TLS configuration; do not expose cleartext Basic credentials on a public interface. Do not enable request-body or Authorization-header logging.

## Test and package

```powershell
cd backend
.\mvnw.cmd clean verify
```

Tests run against H2 in MySQL compatibility mode with `create-drop`, independently of the MySQL database and DB environment variables. They exercise the real Spring Security chain, AuthenticationManager, BCrypt encoder, controllers and JPA repository. H2 does not replace final MySQL smoke testing.

Verified on 3 November 2026 with JDK 21.0.13: 13 integration tests passed, zero failures/errors/skips. Coverage includes registration and BCrypt storage, case-insensitive duplicates, invalid input and malformed JSON, role injection prevention, stateless successful login, incorrect/unknown login, protected paths, database-backed Basic authentication, STUDENT denial, ADMIN access, CORS, and BCrypt password-length enforcement. The MySQL runtime connection still requires local database setup and credentials.

Packaged application: `backend/target/traceroute-backend-0.0.1-SNAPSHOT.jar`. Run with `java -jar target/traceroute-backend-0.0.1-SNAPSHOT.jar` from `backend` after setting DB variables.

## Local demonstration

```powershell
$body = @{name='Student A';email='student@campus.edu';password='StrongPass123!'} | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8080/api/auth/register -Method Post -ContentType application/json -Body $body
Invoke-RestMethod http://127.0.0.1:8080/api/auth/login -Method Post -ContentType application/json -Body $body
```

For a protected request, use `curl.exe -u student@campus.edu http://127.0.0.1:8080/api/users/me`; curl prompts for the password. Login alone does not authenticate later requests.

To demonstrate ADMIN, register a separate local user normally, then have the database administrator run:

```sql
UPDATE traceroute_db.users SET role = 'ADMIN' WHERE email = 'admin@campus.edu';
```

Use that user's credentials with `GET /api/users`. There is no public admin-registration or role-change API and no seeded password.

## Security scope

Sessions and request caching are disabled. CSRF is disabled for this stateless, non-cookie MVP whose frontend explicitly supplies Authorization and omits browser credentials. Statelessness alone does not prevent CSRF: browsers can cache and automatically resend HTTP Basic credentials. Do not use browser-native Basic login prompts as the frontend flow. Reassess and enable CSRF protection if cookies, sessions, browser-managed authentication, or wider deployment are introduced. CORS alone is not CSRF protection.

If startup fails, check JDK 21, DB environment variables, MySQL availability, database existence, account privileges and whether port 8080 is free. Wrapper downloads need network access to Maven Central. No database password is supplied by this repository.
