import test from "node:test";
import assert from "node:assert/strict";
import { makeGraph } from "../src/utils/graph.js";
import { validateReport } from "../src/utils/validation.js";
import { seed, isRelated } from "../src/data.js";
import { normalizeIncident, normalizeGraph } from "../src/utils/contracts.js";

test("backend entity fields map to the frontend incident", () => {
  const incident = normalizeIncident({
    incidentId: 12,
    incidentType: "Phishing",
    suspiciousUrl: "https://example.com",
    senderEmail: "+91 98765 43210",
    sampleIp: "1.2.3.4",
    user: { id: 4, name: "Student A" },
    reportedAt: "2026-11-03T10:00:00",
    status: "PENDING",
  });
  assert.equal(incident.id, "12");
  assert.equal(incident.student, "Student A");
  assert.equal(incident.sender, "+91 98765 43210");
  assert.equal(incident.status, "Pending");
  assert.equal(incident.date, "2026-11-03T10:00:00Z");
});

test("contract graph gains React Flow positions and string IDs", () => {
  const graph = normalizeGraph({
    nodes: [
      { id: 1, label: "Incident 1", type: "incident" },
      { id: 2, label: "example.com", type: "domain" },
    ],
    edges: [{ source: 1, target: 2 }],
  });
  assert.equal(graph.nodes[1].data.kind, "domain");
  assert.equal(graph.edges[0].source, "1");
  assert.equal(graph.edges[0].target, "2");
  assert.equal(typeof graph.nodes[0].position.x, "number");
  assert.throws(
    () =>
      normalizeGraph({ nodes: [{ id: 1 }], edges: [{ source: 1, target: 9 }] }),
    /missing node/,
  );
});

test("shared indicators connect reports without self relationships", () => {
  assert.equal(isRelated(seed[0], seed[0]), false);
  assert.equal(isRelated(seed[0], seed[1]), true);
  assert.equal(isRelated(seed[0], seed[4]), true);
  assert.equal(isRelated(seed[2], seed[3]), false);
});

test("graph shares indicators and students, includes more than eight reports", () => {
  const incidents = Array.from({ length: 12 }, (_, i) => ({
    ...seed[0],
    id: `test-${i}`,
    student: i % 2 ? "Student A" : "Student B",
  }));
  const graph = makeGraph(incidents);
  assert.equal(
    graph.nodes.filter((n) => n.data.kind === "incident").length,
    12,
  );
  assert.equal(graph.nodes.filter((n) => n.data.kind === "student").length, 2);
  assert.equal(graph.nodes.filter((n) => n.data.kind === "domain").length, 1);
  const ids = new Set(graph.nodes.map((n) => n.id));
  assert.equal(ids.size, graph.nodes.length);
  for (const edge of graph.edges) {
    assert(ids.has(edge.source));
    assert(ids.has(edge.target));
  }
  assert.equal(new Set(graph.edges.map((e) => e.id)).size, graph.edges.length);
});

test("empty network is valid", () => {
  assert.deepEqual(makeGraph([]), { nodes: [], edges: [] });
});

const valid = {
  title: "Phishing report",
  description: "A suspicious email requested campus credentials.",
};
test("other incidents require a custom type", () => {
  assert.match(
    validateReport({ ...valid, type: "Other", customType: "  " }),
    /specify/,
  );
  assert.equal(
    validateReport({ ...valid, type: "Other", customType: "SMS phishing" }),
    "",
  );
});
test("report accepts optional indicators and rejects invalid IPv4 and URL schemes", () => {
  assert.equal(validateReport(valid), "");
  assert.equal(
    validateReport({ ...valid, ip: "192.168.1.1", url: "https://example.com" }),
    "",
  );
  assert.match(
    validateReport({ ...valid, ip: "999.1.1.1" }),
    /between 0 and 255/,
  );
  assert.match(
    validateReport({ ...valid, url: "javascript:alert(1)" }),
    /HTTP or HTTPS/,
  );
  assert.match(
    validateReport({ ...valid, description: "   " }),
    /15 characters/,
  );
});

test("evidence validates both type and size", () => {
  assert.equal(
    validateReport({
      ...valid,
      evidence: { type: "application/pdf", size: 1024 },
    }),
    "",
  );
  assert.match(
    validateReport({
      ...valid,
      evidence: { type: "image/png", size: 6 * 1024 * 1024 },
    }),
    /smaller than 5 MB/,
  );
  assert.match(
    validateReport({ ...valid, evidence: { type: "text/html", size: 50 } }),
    /PNG, JPG, or PDF/,
  );
});
