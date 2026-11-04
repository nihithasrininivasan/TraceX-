# TraceX — Campus Cyber Incident Network

Integrated Java 21 / Spring Boot 3.5.16 / MySQL backend and React / Vite / React Flow frontend.

Students register, sign in, report incidents with optional evidence, inspect their reports and visualize shared indicators. Administrators can investigate all reports and update statuses. Credentials use HTTP Basic in frontend memory only; BCrypt hashes are stored in MySQL. Evidence and audit records are database-backed.

## Run locally on Windows

Prerequisites: JDK 21, Node.js 20.19+ or 22.12+, MySQL Server 8 with a local login. No credentials are committed.

```powershell
cd frontend
npm ci
cd ..
.\scripts\Start-TraceX.ps1 -DatabaseUser YOUR_MYSQL_USERNAME
```

The script prompts for the existing MySQL password without echoing or saving it, creates the selected database only if absent, and starts hidden backend/frontend processes. It verifies HTTP/database readiness before printing URLs. Logs and launcher PIDs stay in ignored `.runtime/`. Use `-DatabaseName` to select a dedicated development database. `scripts/Connect-LocalMySql.ps1` provides a masked Windows login dialog for starting only the backend.

- Frontend: **http://localhost:5173/**
- Backend: **http://127.0.0.1:8080**
- Database health query: **http://127.0.0.1:8080/api/health**

These are machine-local URLs, not public deployments. Use the frontend's exact localhost origin to match CORS. Do not use the earlier standalone mock frontend on port 5174 for integrated testing.

Manual backend startup: set JAVA_HOME, DB_USER, DB_PASSWORD and optional DB_URL in the process environment, then run `backend/mvnw.cmd spring-boot:run` from `backend/`. Spring does not automatically load `.env` files. See `backend/.env.example` and [setup details](docs/SETUP.md).

The frontend uses live APIs by default. Explicit `VITE_API_MODE=demo` opts into fictional browser-local sample data; no live errors silently switch to demo mode. Live sign-in is required again after refreshing the page because credentials are not persisted.

## Validate

```powershell
cd backend
.\mvnw.cmd -B -ntp verify
cd ..\frontend
npm test
npm run build
```

Backend automated tests use isolated H2 in MySQL compatibility mode; runtime uses actual MySQL. Tests cover authentication, role restrictions, ownership, evidence, relationships, graph integrity, status updates, audit records and the loopback TCP demonstration. MySQL-backed browser verification is documented separately in [integration results](docs/INTEGRATION.md).

## Integration and ownership

- Teammate 1: authentication, users, security foundation.
- Teammate 2: incident/relationship and networking modules, imported from `a1c31af` into `backend/src/main/java/com/traceroute/`.
- Teammate 3: frontend under `frontend/`, from `985b1fd`.
- Integration adds incident ownership, evidence storage/download, admin status operations, audit logs, health checks, graph entities and API adapters without replacing authentication.

Teammate 2's branch had unrelated Git history and a standalone Boot 4 project. Its Java modules were imported without its duplicate entry point, root build configuration or environment configuration. The existing Boot 3.5.16 foundation remains the only backend build. Original teammate branches are preserved; main is not merged automatically.

Shared indicators are investigative clues, not proof of attribution. Students see only their own reports, graphs and evidence. Administrators see the campus-wide investigation view. Legacy incidents without an owner are visible only to administrators; ownership is never guessed.

See [API contract](docs/API_CONTRACT.md), [entity ownership](docs/ENTITY_CONTRACT.md) and [reference schema](database/schema.sql). Hibernate development schema updates are additive in intent; back up existing databases and review any generated ALTER statements before using a populated nondevelopment database. No DROP/TRUNCATE operation is part of the startup scripts.
