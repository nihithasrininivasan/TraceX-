import test from "node:test";
import assert from "node:assert/strict";
import { createServer } from "vite";

test("plain-user login enables in-memory Basic auth and logout clears it", async () => {
  const vite = await createServer({
    configFile: false,
    server: { middlewareMode: true },
    appType: "custom",
    optimizeDeps: { noDiscovery: true, entries: [] },
    define: { "import.meta.env.VITE_API_MODE": JSON.stringify("live") },
  });
  try {
    const { api, authService, demo, clearCredentials } =
      await vite.ssrLoadModule("/src/services/index.js");
    assert.equal(demo, false);
    const captured = [];
    api.defaults.adapter = async (config) => {
      captured.push(config);
      return {
        data: { id: 1, name: "Test Student", role: "STUDENT" },
        status: 200,
        statusText: "OK",
        headers: {},
        config,
      };
    };
    const credentials = { email: "test@campus.edu", password: "TestPass123!" };
    const result = await authService.login(credentials);
    assert.equal(result.user.name, "Test Student");
    assert.equal(result.token, undefined);
    assert.equal(captured[0].headers.get("Authorization"), undefined);
    await api.get("/users/me");
    assert.equal(
      captured[1].headers.get("Authorization"),
      "Basic " +
        Buffer.from(`${credentials.email}:${credentials.password}`).toString(
          "base64",
        ),
    );
    assert.equal(captured[1].fetchOptions.credentials, "omit");
    await authService.register({ name: "Test Student", ...credentials });
    assert.equal(captured[2].headers.get("Authorization"), undefined);
    clearCredentials();
    await api.get("/users/me");
    assert.equal(captured[3].headers.get("Authorization"), undefined);
  } finally {
    await vite.close();
  }
});
