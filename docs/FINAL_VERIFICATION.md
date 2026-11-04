# Final consistency and API verification — 4 November 2026

Result: all seven requested verification points passed. No production application features or business logic were changed during this pass. Only verification tests, a test runner, and documentation were added or updated.

## Results

| Requested check | Evidence | Result |
| --- | --- | --- |
| Normalized scores | Domain-only, IP-only and combined fixtures stored 0.6000, 0.4000 and 1.0000 in real MySQL; graph/relationship APIs returned 0.6, 0.4 and 1.0; frontend adapter test preserved each value | Pass |
| Foreign keys | Inspected information_schema.KEY_COLUMN_USAGE in both the existing and new verification database | Pass |
| Reports without URLs | Submitted JSON requests with suspiciousUrl omitted, null and blank; all returned 201 with no fabricated domain/IP | Pass |
| Graph duplicates | Checked node IDs, edge IDs, source/target connection pairs and valid endpoints; existing relationship pairs/types were also unique | Pass |
| Audit actors | Checked existing creation logs against report owners; new test records identified the authenticated creator and status-changing admin; denied student updates added no success audit | Pass |
| Role permissions | Anonymous denied; students limited to their own records, evidence and graph; students denied status updates/user listing; administrator access succeeded; public registration could not assign ADMIN | Pass |
| All APIs rerun | Exercised every current controller endpoint over HTTP against an isolated backend connected to actual MySQL | Pass |

### Foreign-key relationships confirmed

| Child column | Referenced column |
| --- | --- |
| incidents.user_id | users.id |
| incident_relationships.source_incident_id | incidents.incident_id |
| incident_relationships.target_incident_id | incidents.incident_id |
| evidence.incident_id | incidents.incident_id |
| audit_logs.user_id | users.id |

No orphan evidence/relationship records or invalid normalized scores were found in the existing database. Legacy ownerless incidents remain a separate compatibility case; new reports derive their owner from authentication.

## Runs

- Maven verify: **23 tests passed**, zero failures/errors/skips; backend packaging succeeded.
- Frontend tests: **10 tests passed**, zero failures; includes explicit score-preservation coverage.
- Real-MySQL verification: **63 checks passed**, zero failures.
- Existing frontend and database health endpoints were checked after verification.

The API verification used a new database, **tracex_verify_c94eb0786334**, on the existing MySQL server. The main traceroute_db database was inspected read-only. Verification students/admin, reports, evidence and audit records were created only in the new database. Its temporary backend was stopped afterward. The verification database was retained for review; no database was dropped. Credentials were held in process memory and were not printed or committed.

## API coverage

- POST /api/auth/register; POST /api/auth/login (valid, invalid and duplicate cases).
- GET /api/users/me; GET /api/users (student denial/admin success).
- GET /api/health with a database query.
- POST /api/incidents in JSON and multipart formats, including evidence and missing/null/blank URLs.
- GET /api/incidents; GET /api/incidents/{id}; GET /api/incidents/{id}/related.
- GET /api/incidents/{id}/evidence/{fileId}, checking original bytes and permissions.
- PUT /api/incidents/{id}/status, checking permissions, persistence and audit actor.
- GET /api/graph; GET /api/network/graph, checking equivalent node IDs and permissions.
- GET /api/relationships; GET /api/relationships/incident/{id}.
- GET /api/network/dns; GET /api/network/ip; GET /api/network/ip/count; GET /api/network/tcp.
- OPTIONS /api/incidents for the allowed frontend origin.

## Reproduce

Run backend `mvnw.cmd -B -ntp verify` and frontend `npm test`. For the real-MySQL verification, run `scripts/Connect-LocalMySql.ps1 -FinalVerification` from the repo root and enter the existing local MySQL login in the masked dialog. This requires permission to create a new verification database. Each run uses a fresh database name and never drops an existing database. The local detailed result is `.runtime/final-verification.json` (ignored by Git).

Scores are rule-based indicator-match weights, not probabilities of common attacker identity. The UI currently shows relationship labels, not numeric percentages; if percentages are added later, multiply normalized scores by 100 exactly once.
