import { api, demo } from "./api";
import { seed, isRelated } from "../data";
import { normalizeIncident, normalizeGraph } from "../utils/contracts";

const storageKey = "tracex-incidents";
function read() {
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey));
    return Array.isArray(saved) ? saved : structuredClone(seed);
  } catch {
    return structuredClone(seed);
  }
}
function write(incidents) {
  localStorage.setItem(storageKey, JSON.stringify(incidents));
}
function requireArray(data) {
  // Accept either the agreed array or Spring Data's default page wrapper.
  const incidents = Array.isArray(data) ? data : data?.content;
  if (!Array.isArray(incidents))
    throw new Error("Expected an incident array from the API.");
  return incidents.map(normalizeIncident);
}

export const incidentService = {
  async downloadEvidence(incidentId, fileId) {
    return (
      await api.get(
        `/incidents/${encodeURIComponent(incidentId)}/evidence/${encodeURIComponent(fileId)}`,
        { responseType: "blob" },
      )
    ).data;
  },
  async list() {
    return demo ? read() : requireArray((await api.get("/incidents")).data);
  },
  async create(body) {
    if (!demo) {
      const form = new FormData();
      const { evidence } = body;
      // Incident request DTO is proposed until teammate 2 finalizes it.
      const details = {
        incidentType: body.type === "Other" ? body.customType : body.type,
        description: body.description,
        suspiciousUrl: body.url,
        senderEmail: body.sender,
        sampleIp: body.ip,
        title: body.title,
        severity: body.severity,
        customType: body.customType,
      };
      form.append(
        "incident",
        new Blob([JSON.stringify(details)], { type: "application/json" }),
      );
      if (evidence) form.append("evidence", evidence);
      return normalizeIncident((await api.post("/incidents", form)).data);
    }
    let domain = "";
    try {
      domain = new URL(body.url).hostname.toLowerCase();
    } catch {
      /* No URL supplied. */
    }
    const incident = {
      ...body,
      evidence: body.evidence?.name || "",
      domain,
      id: `TX-${crypto.randomUUID().slice(0, 8).toUpperCase()}`,
      date: new Date().toISOString(),
      status: "Pending",
    };
    write([incident, ...read()]);
    return incident;
  },
  async status(id, status) {
    if (!["Pending", "Investigating", "Resolved"].includes(status))
      throw new Error("Invalid status.");
    if (!demo)
      return (
        await api.put(`/incidents/${encodeURIComponent(id)}/status`, { status })
      ).data;
    const incidents = read();
    if (!incidents.some((incident) => incident.id === id))
      throw new Error("Incident not found.");
    write(
      incidents.map((incident) =>
        incident.id === id ? { ...incident, status } : incident,
      ),
    );
  },
  async details(id) {
    const incident = demo
      ? read().find((entry) => entry.id === id)
      : normalizeIncident(
          (await api.get(`/incidents/${encodeURIComponent(id)}`)).data,
        );
    if (!incident) throw new Error("Incident not found.");
    return incident;
  },
  async related(id) {
    if (!demo)
      return requireArray(
        (await api.get(`/incidents/${encodeURIComponent(id)}/related`)).data,
      );
    const incidents = read();
    const incident = incidents.find((entry) => entry.id === id);
    if (!incident) return [];
    return incidents.filter((entry) => isRelated(incident, entry));
  },
  async graph() {
    if (demo) return null;
    const { data } = await api.get("/graph");
    if (!Array.isArray(data?.nodes) || !Array.isArray(data?.edges))
      throw new Error("Invalid network graph response.");
    return normalizeGraph(data);
  },
};
