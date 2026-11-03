# Frontend — teammate 3

React UI is owned by teammate 3. Development origin: `http://localhost:5173`. Backend: `http://127.0.0.1:8080`.

See [API contract](../docs/API_CONTRACT.md). Registration/login return user details, with no token or session. Send an explicit `Authorization: Basic <base64(email:password)>` header on each protected request. Use fetch `credentials: 'omit'`. Keep credentials in memory only and clear them on logout; never persist them in localStorage, sessionStorage, URLs, or logs. Base64 is encoding, not encryption. Only use this local demonstration over loopback HTTP, or HTTPS for nonlocal hosting.
