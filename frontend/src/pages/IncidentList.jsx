import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import PageHead from "../components/PageHead";

import IncidentTable from "../components/IncidentTable";
function IncidentList({ items, admin }) {
  const [q, setQ] = useState(""),
    [status, setStatus] = useState("All statuses");
  let list = items.filter(
    (x) =>
      (status === "All statuses" || x.status === status) &&
      `${x.title} ${x.id} ${x.domain} ${x.student} ${x.type} ${x.customType || ""}`
        .toLowerCase()
        .includes(q.toLowerCase()),
  );
  return (
    <>
      <PageHead
        eyebrow={admin ? "INVESTIGATION WORKSPACE" : "CAMPUS REPORTS"}
        title={admin ? "Admin console" : "Incident reports"}
        desc={
          admin
            ? "Review evidence, investigate connections, and update incident statuses."
            : "Every report is a step toward a safer campus."
        }
      >
        <Link to="/report" className="button primary">
          <Plus size={17} />
          Report incident
        </Link>
      </PageHead>
      <section className="panel">
        <div className="list-toolbar">
          <label className="search">
            <Search size={17} />
            <input
              aria-label="Search incidents"
              placeholder="Search incidents, indicators, students…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </label>
          <select
            aria-label="Filter status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {["All statuses", "Pending", "Investigating", "Resolved"].map(
              (x) => (
                <option key={x}>{x}</option>
              ),
            )}
          </select>
          <span className="muted">{list.length} reports</span>
        </div>
        <IncidentTable items={list} />
      </section>
    </>
  );
}
export default IncidentList;
