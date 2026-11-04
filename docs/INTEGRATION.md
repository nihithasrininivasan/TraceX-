# Integration provenance and verification

## Resumed verification — 4 November 2026

The initial login retry on 4 November was rejected; a later retry succeeded and restored the backend's MySQL connection. A subsequent final pass completed 23 backend tests, 10 frontend tests and 63 real-MySQL/API checks, with zero failures. The main database's constraints and records were inspected read-only; API fixtures used a separate verification database. See [FINAL_VERIFICATION.md](FINAL_VERIFICATION.md) for current results and endpoint coverage. Password recovery was not needed. No credentials or runtime files are committed.

Dedicated branch: `codex/integrate-tracex`. Source modules: foundation b581198, frontend 985b1fd, incident/networking a1c31af. The incident branch has unrelated history and a standalone Boot 4 layout; its Java modules were imported into the existing backend without importing duplicate entry points, root Maven/config files, or changing the Boot 3.5.16/Java 21 foundation. Teammate branches/main were not overwritten.

## Changes

- Connected incident records to authenticated User ownership and returned DTOs without credentials/hashes.
- Added owner/admin access checks, ADMIN-only status updates and audit logs.
- Added transactional PNG/JPEG/PDF evidence storage/download with size/signature checks.
- Preserved teammate domain/IP matching, normalized scores to [0,1], and added foreign-key mappings.
- Built permission-filtered user/incident/domain/URL/IP graph nodes from actual records, plus stored relationship edges.
- Integrated JSON/multipart report requests, aliases for /api/graph and /api/network/graph, related reports and useful error responses.
- Kept networking demonstrations, bounded TCP to loopback/timeouts, and filtered IP analysis by caller visibility.
- Defaulted frontend to live APIs, added authorized evidence downloads and aligned Other/contact fields.
- Added masked local MySQL prompts, startup verification and a database health query. No user password is printed or committed.

## Automated verification

- Backend: 20 tests (13 original authentication tests plus 7 incident/network integration tests), isolated H2 MySQL-compatibility profile.
- Frontend: 9 tests covering Basic auth memory handling, contract adapters, graph integrity and report validation.
- Frontend production build and backend Maven packaging pass.
- Live HTTP smoke: 20 checks against actual MySQL, including student/admin auth, persisted UI-created reports/evidence bytes, role escalation rejection, cross-student access denial, ADMIN status updates, shared-domain graph across students, related reports, graph edge integrity, sample DNS/TCP, allowed CORS and rejection of an unapproved origin.

Initial failures were resolved: duplicate relationship column mappings were corrected with explicit @Column names; a graph test's JSONPath assertion was corrected; launcher detection was updated for Maven's Java argument-file process. No failing result was counted as passing.

A final restart attempt was not completed because the last local password entry was rejected. It did not stop or change the already-running MySQL-backed server. That running server passed the live smoke checks; the final source/package passed all automated tests. Restarting the packaged build later requires the correct local MySQL login through the provided prompt.

## Actual MySQL / browser checks

The existing MySQL80 service and user-provided local login were used; MySQL 8.0.46 connected. Direct inspection confirmed users, incidents, incident_relationships, evidence and audit_logs tables. After the first two UI submissions, there were two sample incidents, one relationship, one evidence record and two audit records. Later smoke checks added a separate student's safe report and updated a status.

Browser checks: actual student registration/login; empty dashboard/graph; PNG evidence upload/download; two reports sharing https://fakebank.test/login; Other → SMS phishing; phone sender contact; related report details; real React Flow shared domain and stored relationship edge; node inspector. The database owner explicitly approved a newly registered local QA administrator. No existing user role was changed. QA accounts/data remain in the local development database; generated credentials are not included here.

## Limits

Localhost only; no public deployment or remote port forwarding was configured. .runtime data and build outputs are not committed. Production authentication/storage hardening, evidence malware scanning, pagination, managed migrations and formal investigation workflow transitions remain outside the semester MVP. Shared-indicator matches remain clues rather than proof of attribution.
