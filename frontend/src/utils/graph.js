// Authoritative detection belongs to the API; this builds illustrative connections.
export function makeGraph(items) {
  const nodes = [],
    edges = [],
    students = new Map(),
    indicators = new Map();
  const connect = (source, target) =>
    edges.push({
      id: JSON.stringify([source, target]),
      source,
      target,
      style: { stroke: "#465b7b", strokeWidth: 1.2 },
    });
  items.forEach((incident, index) => {
    const studentId = `student:${incident.studentId || incident.student}`;
    const incidentId = `incident:${incident.id}`;
    if (!students.has(studentId)) {
      const node = {
        id: studentId,
        type: "entity",
        position: { x: 0, y: students.size * 135 },
        data: {
          kind: "student",
          label: incident.student,
          detail: "Student connected through submitted reports.",
        },
      };
      students.set(studentId, node);
      nodes.push(node);
    }
    nodes.push({
      id: incidentId,
      type: "entity",
      position: { x: 290, y: index * 135 + 25 },
      data: {
        kind: "incident",
        label: incident.id,
        detail: incident.title,
        incident,
      },
    });
    connect(studentId, incidentId);
    ["domain", "url", "ip"].forEach((kind) => {
      if (!incident[kind]) return;
      const value = incident[kind].trim();
      const indicatorId = `${kind}:${value}`;
      if (!indicators.has(indicatorId)) {
        const node = {
          id: indicatorId,
          type: "entity",
          position: {
            x: kind === "ip" ? 925 : 610,
            y: indicators.size * 110 - 40,
          },
          data: {
            kind,
            label: value,
            detail:
              "Reported indicator. Shared indicators may connect multiple reports.",
          },
        };
        indicators.set(indicatorId, node);
        nodes.push(node);
      }
      connect(incidentId, indicatorId);
    });
  });
  return { nodes, edges };
}
