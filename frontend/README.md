# TraceX frontend — Teammate 3

React + Vite, React Router, Axios and React Flow. Contains login, registration, overview, incident reporting, searchable reports, details, network visualization and role-aware admin controls. The UI uses a blue gradient and minimal dark surfaces.

## Run locally

Requires Node.js 20.19+ or 22.12+.

```sh
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Use this exact origin: the backend CORS contract permits it. The server refuses a busy port instead of silently switching to an unsupported origin.

Demo mode is the default. Student/Admin demo access is explicitly separate from actual authentication. Fictional demo reports persist in localStorage, demo identity in sessionStorage, and demo evidence stores its filename only. No real credentials are persisted.

```sh
npm test
npm run build
npm run format
```

The build output is `dist/`. Hash routes support static hosting without route rewrites.

## Connect implemented authentication APIs

Copy `.env.example` to `.env.local` and change:

```dotenv
VITE_API_MODE=live
VITE_API_BASE_URL=http://127.0.0.1:8080/api
```

Restart Vite. Start the backend and MySQL using the repository's backend instructions. Registration and login match [API_CONTRACT.md](../docs/API_CONTRACT.md): both return a plain user object, without a token or session. Protected requests use an explicit HTTP Basic Authorization header. Axios uses its fetch adapter with `credentials: 'omit'`. Credentials stay only in module memory and are cleared on logout or 401. Refreshing in live mode requires signing in again. Use loopback HTTP for the local demonstration or HTTPS for remote deployment.

The backend derives roles and identity. Frontend role checks only hide controls; backend authorization remains required. No authentication/security/backend files were changed for this frontend.

## Incident integration — pending Teammate 2

Incident endpoints are reserved, not yet implemented. Live authentication can work while incident loading returns a backend error; demo mode supports the full presentation until those APIs are ready. Live API errors never silently switch to demo writes.

The graph service calls `/api/graph` and converts the documented `{nodes:[{id,label,type}],edges:[{source,target}]}` shape into React Flow nodes with string IDs, positions and details. A failed graph endpoint shows a notice and derives connections from already loaded incident data.

Incident responses accept `incidentId`, `incidentType`, `suspiciousUrl`, `senderEmail`, `sampleIp`, `reportedAt` and `user:{id,name}` from [ENTITY_CONTRACT.md](../docs/ENTITY_CONTRACT.md). UTC local timestamps receive a `Z` suffix. Optional title and severity use display fallbacks if absent. List responses may be arrays or Spring Page `content`; page navigation is not implemented yet, so return the complete permitted list for the demo.

**Proposals requiring agreement with Teammate 2:**

- Report creation currently sends multipart `incident` (JSON Blob) plus optional `evidence` (PNG/JPG/PDF, maximum 5 MB).
- Incident JSON uses `incidentType`, `description`, `suspiciousUrl`, `senderEmail`, `sampleIp` and optional `title`, `severity`, `customType`. It does not send the reporter identity. The backend must derive it from authentication.
- Selecting Other requires a custom description; this is sent as the incidentType and also preserved in customType.
- The sender input accepts an email **or phone number**. Agree on a contact field/storage length and validation; the reference entity calls this senderEmail.
- `/api/incidents/:id/related` is expected to return permitted incident records.
- `PUT /api/incidents/:id/status` currently sends `{status}` with Pending, Investigating or Resolved. The server must enforce ADMIN access and permitted transitions.
- Evidence preview/download needs an agreed authenticated endpoint; there is no file retrieval implementation yet.

## Source layout

```text
src/
  components/    Shared UI, authentication form, navigation, network graph
  pages/         Login, Register, Dashboard, ReportIncident, IncidentList,
                 IncidentDetails, IncidentNetwork, AdminDashboard
  services/      Axios client, authService, incidentService
  utils/         Contract adapters, graph building, validation, formatting
  App.jsx        Layout, authentication state, routes
  main.jsx       Entry point
  data.js        Fictional demo records
  style.css      Responsive styling
tests/           Frontend logic and contract checks
```

Shared indicators illustrate potentially related incidents, not confirmed attribution. The backend owns detection, visibility, authorization, database persistence and investigation rules.
