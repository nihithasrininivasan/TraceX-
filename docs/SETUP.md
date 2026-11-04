# Local integrated setup

Install JDK 21, Node.js 20.19+ or 22.12+, and MySQL Server 8. Keep the existing local MySQL service and data; no separate database engine is substituted for runtime. Tests alone use the repository's isolated H2 profile.

1. In `frontend/`, run `npm ci`.
2. From the repository root, run `scripts/Start-TraceX.ps1 -DatabaseUser YOUR_USERNAME`. The MySQL password is collected through a masked terminal prompt and passed to the backend process environment only.
3. Open **http://localhost:5173/** and register a new student. Sign in, then report a safe sample URL such as `https://fakebank.test/login`.
4. Check **http://127.0.0.1:8080/api/health** for a real database query.

`-DatabaseName` defaults to traceroute_db; `-DatabaseHost`, `-DatabasePort` and `-JavaHome` can be supplied. Scripts create the chosen database only if absent. The login needs permissions to create the development database/tables and perform CRUD. Use an application-specific account with appropriate development privileges when available; no password is supplied by the repository. For an existing database without CREATE DATABASE privileges, create it through your DBA first and start the backend manually with environment variables.

The alternative `scripts/Connect-LocalMySql.ps1` opens a local Windows masked login dialog for starting only the backend. It uses the installed MySQL 8.0 and JDK 21 paths on this computer. Its optional TestAdminEmail mode is restricted to freshly registered `qa.admin.<uuid>@campus.test` test accounts, requires a separate DBA login and is never a public application endpoint. Creation of such a local administrator requires the database owner's approval. No existing user's role is changed by this mode.

## Manual startup

Set JAVA_HOME to the installed JDK 21 directory. In PowerShell, set DB_USER and optional DB_URL (`jdbc:mysql://127.0.0.1:3306/traceroute_db`). Collect the existing password using `Read-Host -AsSecureString` and assign DB_PASSWORD from its secure value, without printing it. Then run `backend/mvnw.cmd spring-boot:run` from `backend/`.

In another shell, run `npm run dev` from `frontend/`. It uses strict port 5173 to match CORS. The former standalone demo server on 5174 is separate and is not the integrated application.

Live APIs are the frontend default. VITE_API_BASE_URL may override the API location; explicit VITE_API_MODE=demo is reserved for the fictional demo. Restart Vite after changing env files. No real credentials are persisted by the frontend.

## Validation and security

Run `backend/mvnw.cmd -B -ntp verify` from backend and `npm test` / `npm run build` from frontend. Isolated automated tests require no MySQL credentials and never truncate the live database. Runtime MySQL smoke results are in INTEGRATION.md.

Students can access only their own reports/evidence/graph nodes. Admins can inspect all reports and change status. New registrations always receive STUDENT. There is no public admin-registration/role-change API. For a real admin account, a DBA must approve and assign the role to the intended account; do not give every student admin access for a demonstration.

Backend binds only to 127.0.0.1. Use HTTPS and deliberate hosting configuration before exposing HTTP Basic remotely. CORS does not replace authentication. Sessions and request caching remain disabled; credentials are explicitly sent by the fetch adapter with browser credentials omitted. If switching to cookies or browser-managed authentication, reassess CSRF protections rather than merely copying this stateless configuration.

Development schema management uses Hibernate update, with SQL initialization disabled. No DROP/TRUNCATE is executed. Back up and inspect a populated legacy schema before migration, particularly ownership, old relationship score columns and evidence storage. Do not run database/schema.sql as a second schema initializer.

Runtime logs/PIDs are in ignored `.runtime/`; Maven target/ and frontend dist/node_modules are ignored. Passwords are not printed or committed. Restart scripts stop only a process identified by this checkout's TraceX startup logs and refuse to stop an unrelated port-8080 listener.
