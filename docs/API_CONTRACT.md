# Integrated API contract

Base URL: `http://127.0.0.1:8080/api`. Frontend origin: `http://localhost:5173`.

## Authentication

- `POST /auth/register` `{name,email,password}` → 201 `{id,name,email,role}`; always STUDENT.
- `POST /auth/login` `{email,password}` → 200 same public user. No token/session.
- `GET /users/me` → authenticated public user.
- `GET /users` → ADMIN-only public user array.

Send explicit `Authorization: Basic <base64(email:password)>` for protected requests. Frontend keeps credentials only in memory, uses fetch `credentials: omit`, and clears them on logout/401. Use loopback HTTP for local development or HTTPS for remote deployment. BCrypt passwords are never returned. Registration allows 8–72 printable ASCII characters; login allows 1–72. Names max 100; emails max 254.

## Incidents

Contact validation is enforced in the UI and API. Account and sender emails require a dotted domain with a 2–63 letter suffix, valid domain labels, and a local part of at most 64 characters (no leading, trailing, or consecutive dots); the full address is at most 254 characters. Sender contact is optional and surrounding whitespace is trimmed. Phone contacts require 10 digits without a country code, or 8–15 digits with a leading `+` country code; the first digit must be nonzero. Spaces, hyphens, and balanced 2–5 digit area-code parentheses after the initial digit group are allowed. Letters, extensions, repeated separators, and embedded control characters are rejected. These checks validate format, not ownership or deliverability. Both JSON and multipart incident requests reject invalid contacts with HTTP 400.

- `GET /incidents`: permitted records, newest first. STUDENT sees own; ADMIN sees all.
- `POST /incidents`: JSON or multipart with `incident` application/json part and optional `evidence` file.
- `GET /incidents/{id}`: permitted report; unauthorized/nonexistent IDs return 404.
- `GET /incidents/{id}/related`: related records also permitted to the caller.
- `PUT /incidents/{id}/status`: ADMIN only, `{status:"Pending"|"Investigating"|"Resolved"}`; uppercase accepted. Response status uses uppercase.
- `GET /incidents/{id}/evidence/{fileId}`: owner/admin-only download, attachment disposition and nosniff.

Incident request:

```json
{"incidentType":"Phishing","title":"Suspicious scholarship email","description":"A sufficiently detailed incident description.","suspiciousUrl":"https://fakebank.test/login","senderEmail":"sender@example.test","sampleIp":"192.0.2.10","severity":"High","customType":null}
```

The authenticated principal determines the owner; client-supplied user identity is ignored. incidentType is required, max 100. Description: 15–1000. Optional title max 140, URL max 255, sender contact max 254, customType max 100. URL is HTTP(S) only; it is never fetched. sampleIp is optional IPv4. severity is Low/Medium/High. The compatibility field `senderEmail` accepts email or phone contact. Selecting Other in the UI requires customType; the frontend also sends that custom value as incidentType.

Response: `{incidentId,incidentType,title,description,suspiciousUrl,domain,senderEmail,sampleIp,status,reportedAt,severity,customType,user:{id,name},evidenceFiles:[{id,filename,contentType}]}`. No user email/hash is disclosed through incident DTOs. Times are UTC. Frontend normalizes entity field names and statuses.

Evidence: PNG/JPEG/PDF, maximum 5 MB, media type and file signature checked. Bytes are stored in MySQL; filenames are sanitized. Reporting, evidence, relationship detection and audit writes share one transaction.

## Network and relationships

- `GET /graph` and `GET /network/graph`: aliases for the same permission-filtered database graph.
- `GET /relationships`: links with both endpoints visible.
- `GET /relationships/incident/{id}`: visible links for an accessible report.
- `GET /network/dns?domain=fakebank.test`: explicit SAMPLE_DNS demonstration; no external DNS requests.
- `GET /network/ip?address=192.0.2.10` and `/network/ip/count`: caller-visible incident matches/count.
- `GET /network/tcp?message=hello`: bounded loopback-only TCP client/server demonstration.

Graph shape: `{nodes:[{id,label,type,position:{x,y},data:{kind,label,detail,incident?}}],edges:[{id,source,target,label?,data?}]}`. Kinds: student, incident, domain, url, ip. IDs are namespaced strings. Related incidents share indicator nodes; explicit stored relationship edges carry the matched indicator and score in [0,1]. Every edge endpoint is included in the filtered graph.

Sample DNS maps fakebank.test/fakegift.test to 192.0.2.10 and fakecollege.test to 192.0.2.30. User-supplied sampleIp takes precedence. The teammate's relationship algorithm scores shared domains 0.6 and shared IPs 0.4. It does not claim verified attribution.

## Health and errors

`GET /health` is public and returns `{status:"UP",database:"UP"}` only after a SELECT 1 succeeds. Other protected paths require Basic authentication. Error bodies are `{status,message,errors?}`. Common responses: 400 validation, 401 authentication, 403 admin action denied/CORS, 404 inaccessible resource, 409 duplicate registration, 413 oversized evidence. No credentials are logged by application code.
