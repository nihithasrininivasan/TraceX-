# TraceX React frontend

Run `npm ci`, then `npm run dev` from this directory. Open **http://localhost:5173/**. Java backend: `http://127.0.0.1:8080/api`. The exact frontend origin is required by backend CORS.

Live APIs are the default. Copy `.env.example` to `.env.local` to override API location. Explicit `VITE_API_MODE=demo` enables fictional browser-local demo data. Live mode never falls back to demo writes.

Registration/login use the backend's plain-user response. Protected requests send HTTP Basic with credentials held only in module memory and fetch `credentials: omit`. Refreshing live mode requires signing in again. No real password/header is stored in localStorage or sessionStorage.

Implemented pages: Login, Register, Dashboard, ReportIncident, IncidentList, IncidentDetails, IncidentNetwork and AdminDashboard. Reports support Other/custom types and sender email or phone number. PNG/JPEG/PDF evidence uploads use multipart and evidence downloads use authenticated Blob requests. Status updates require an administrator on the server.

React Flow receives `/api/graph` data from the database. Contract adapters normalize IDs, entity fields, statuses, UTC timestamps and graph positions. If graph fetching fails, a visible warning accompanies a graph derived only from incident records already retrieved from the backend. Empty datasets are supported.

`npm test` runs logic/contract tests; `npm run build` emits `dist/`; `npm run format` formats source. Components, pages, services and utilities are separated under `src/`. See [API contract](../docs/API_CONTRACT.md) and [project setup](../README.md).
