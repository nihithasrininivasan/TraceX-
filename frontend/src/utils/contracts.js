export function normalizeIncident(value) {
  const status = String(value.status || "Pending")
    .toLowerCase()
    .replaceAll("_", " ");
  return {
    ...value,
    id: String(value.incidentId ?? value.id),
    type: value.incidentType ?? value.type ?? "Other",
    title:
      value.title || `${value.incidentType || value.type || "Incident"} report`,
    url: value.suspiciousUrl ?? value.url ?? "",
    sender: value.senderEmail ?? value.sender ?? "",
    ip: value.sampleIp ?? value.ip ?? "",
    student: value.user?.name ?? value.student ?? "Campus student",
    studentId: value.user?.id ?? value.studentId,
    date: value.reportedAt
      ? /Z$|[+-]\d{2}:\d{2}$/.test(value.reportedAt)
        ? value.reportedAt
        : value.reportedAt + "Z"
      : value.date,
    status:
      {
        pending: "Pending",
        investigating: "Investigating",
        resolved: "Resolved",
      }[status] ||
      value.status ||
      "Pending",
    severity: value.severity || "Unspecified",
    evidenceFiles: value.evidenceFiles || [],
  };
}

export function normalizeGraph(graph) {
  if (!Array.isArray(graph?.nodes) || !Array.isArray(graph?.edges))
    throw new Error("Invalid network graph response.");
  const ids = new Set(graph.nodes.map((node) => String(node.id)));
  if (ids.size !== graph.nodes.length)
    throw new Error("Graph has duplicate node IDs.");
  const nodes = graph.nodes.map((node, index) => ({
    id: String(node.id),
    type: "entity",
    position: node.position || {
      x: (index % 3) * 290,
      y: Math.floor(index / 3) * 135,
    },
    data: node.data || {
      kind: ["student", "incident", "domain", "url", "ip"].includes(node.type)
        ? node.type
        : "incident",
      label: node.label || String(node.id),
      detail: node.description || "Reported network entity.",
    },
  }));
  const edges = graph.edges.map((edge, index) => {
    const source = String(edge.source),
      target = String(edge.target);
    if (!ids.has(source) || !ids.has(target))
      throw new Error("Graph edge refers to a missing node.");
    return { ...edge, id: String(edge.id ?? `edge-${index}`), source, target };
  });
  return { nodes, edges };
}
