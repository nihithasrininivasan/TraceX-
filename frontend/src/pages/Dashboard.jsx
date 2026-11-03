import React from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, Plus, Network, FileText } from "lucide-react";
import NetworkGraph from "../components/NetworkGraph";
import Badge from "../components/Badge";
import { isRelated } from "../data";
import { stamp } from "../utils/format";

export default function Dashboard({ items }) {
  const open = items.filter(
    (incident) => incident.status !== "Resolved",
  ).length;
  const resolved = items.length - open;
  const connected = items.filter((incident) =>
    items.some((other) => isRelated(incident, other)),
  ).length;

  return (
    <div className="overview">
      <section className="security-hero">
        <div className="hero-grid" aria-hidden="true" />
        <div className="hero-copy">
          <span className="hero-label">
            <span className="signal-dot" /> CAMPUS THREAT INTELLIGENCE
          </span>
          <h1>
            See the signal.
            <br />
            Stop the threat.
          </h1>
          <p>A safer campus starts with a single report.</p>
          <Link className="button hero-button" to="/report">
            <Plus size={17} /> Report an incident <ArrowUpRight size={17} />
          </Link>
        </div>
        <div className="hero-summary">
          {[
            [items.length, "Total reports"],
            [open, "Open cases"],
            [resolved, "Resolved"],
          ].map(([value, label]) => (
            <div key={label}>
              <strong>{String(value).padStart(2, "0")}</strong>
              <span>{label}</span>
            </div>
          ))}
        </div>
      </section>

      <div className="overview-section-heading">
        <h2>Your security workspace</h2>
        <span>Report. Connect. Investigate.</span>
      </div>
      <div className="dashboard-grid">
        <section className="panel recent-panel">
          <div className="panel-head">
            <h2>
              Recent reports <span className="count">{items.length}</span>
            </h2>
            <Link
              className="round-link"
              to="/incidents"
              aria-label="View all incident reports"
            >
              <ArrowUpRight size={18} />
            </Link>
          </div>
          <div className="report-feed">
            {items.slice(0, 4).map((incident) => (
              <Link
                className="feed-item"
                key={incident.id}
                to={"/incidents/" + incident.id}
              >
                <span className="feed-icon">
                  <FileText size={17} strokeWidth={1.5} />
                </span>
                <div className="feed-copy">
                  <strong>{incident.title}</strong>
                  <span>
                    {incident.id} <b>·</b> {stamp(incident.date)}
                  </span>
                </div>
                <Badge value={incident.status} />
              </Link>
            ))}
            {!items.length && (
              <div className="empty">Your reports will appear here.</div>
            )}
          </div>
          <Link className="feed-footer" to="/incidents">
            View all reports <ArrowUpRight size={15} />
          </Link>
        </section>
        <section className="panel graph-panel">
          <div className="panel-head">
            <h2>
              <Network size={17} /> Connected signals
            </h2>
            <Link
              className="round-link"
              to="/network"
              aria-label="Explore incident network"
            >
              <ArrowUpRight size={18} />
            </Link>
          </div>
          <NetworkGraph items={items.slice(0, 3)} compact />
          <Link className="feed-footer" to="/network">
            <span>{connected} reports share an indicator</span>
            <span>
              Explore network <ArrowUpRight size={15} />
            </span>
          </Link>
        </section>
      </div>
    </div>
  );
}
