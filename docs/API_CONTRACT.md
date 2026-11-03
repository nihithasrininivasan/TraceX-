# API contract

Base URL: `http://127.0.0.1:8080`. JSON request/response bodies. Emails are normalized to lowercase and trimmed in services. Validation checks the submitted email first; send email without surrounding whitespace. Names are trimmed. Passwords are never trimmed or returned.

## Implemented endpoints

| Method | Path | Access | Success |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | 201 user |
| POST | `/api/auth/login` | Public | 200 user |
| GET | `/api/users/me` | Any authenticated user | 200 user |
| GET | `/api/users` | ADMIN | 200 array of users ordered by id |

Registration body:

```json
{"name":"Student A","email":"student@campus.edu","password":"StrongPass123!"}
```

Login body:

```json
{"email":"student@campus.edu","password":"StrongPass123!"}
```

User response (no password, hash, token, or session):

```json
{"id":1,"name":"Student A","email":"student@campus.edu","role":"STUDENT"}
```

Registration always assigns STUDENT, regardless of any submitted role field. Name: nonblank, maximum 100 characters. Email: valid, nonblank, maximum 254 characters. Registration password: 8–72 printable ASCII characters; login password: 1–72 printable ASCII characters. The ASCII restriction makes the BCrypt 72-byte limit explicit and prevents silent multibyte truncation.

Login uses Spring Security AuthenticationManager. A successful login validates credentials only. Send `Authorization: Basic <base64(email:password)>` separately on every protected request. Use only loopback HTTP or HTTPS. Never store credentials in frontend localStorage or sessionStorage; keep them in memory for the local demonstration.

## Errors

| Status | Condition |
| --- | --- |
| 400 | Invalid input or malformed JSON |
| 401 | Invalid login, missing or invalid Basic credentials |
| 403 | Authenticated STUDENT requests admin endpoint; disallowed CORS origin |
| 409 | Email already registered, including differently cased duplicates |

Application validation/authentication errors use:

```json
{"status":400,"message":"Validation failed","errors":{"email":"must be a well-formed email address"}}
```

Other application errors use an empty `errors` object. Security-filter 401 responses include `WWW-Authenticate: Basic realm="TraceRoute"`. CORS rejections and framework routing errors may use framework error formats. No rejected password values are included in validation responses.

CORS allows `http://localhost:5173`, explicit Authorization and Content-Type headers, and GET/POST/PUT/PATCH/DELETE/OPTIONS. Browser credential mode is disabled; use `credentials: 'omit'` and an explicit header. Preflight requests do not need Basic authentication.

## Reserved teammate 2 endpoints — not implemented

All `/api/incidents/**` and `/api/graph/**` paths require authentication already. Payload validation, ownership enforcement, incident status rules and business errors must be defined by teammate 2. Authentication does not yet imply an incident authorization policy.

| Method | Path | Intended behavior |
| --- | --- | --- |
| POST | `/api/incidents` | Report incident; derive user from authenticated principal |
| GET | `/api/incidents` | List permitted incidents |
| GET | `/api/incidents/{id}` | Read permitted incident |
| GET | `/api/graph` | Return graph |

Required graph shape:

```json
{"nodes":[{"id":1,"label":"Incident 1","type":"incident"}],"edges":[{"source":1,"target":2}]}
```

The example shows the node/edge field shape; actual edge endpoints must correspond to nodes included in the graph. No placeholder incident or graph controllers are installed.
