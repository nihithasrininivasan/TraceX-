import React, { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowUpRight, ChevronRight, Link2 } from "lucide-react";
import PageHead from "../components/PageHead";
import { service, demo } from "../services";
import Badge from "../components/Badge";
import { stamp } from "../utils/format";
function Details({ user, refresh }) {
  const { id } = useParams(),
    [item, setItem] = useState(null),
    [related, setRelated] = useState([]),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true;
    setItem(null);
    setRelated([]);
    setError("");
    Promise.all([service.details(id), service.related(id)])
      .then(([x, r]) => {
        if (!active) return;
        if (!x) throw Error("Incident not found");
        setItem(x);
        setRelated(r);
      })
      .catch((e) => {
        if (active) setError(e.message);
      });
    return () => {
      active = false;
    };
  }, [id]);
  async function update(status) {
    setBusy(true);
    setError("");
    try {
      await service.status(id, status);
      setItem({ ...item, status });
      await refresh();
    } catch {
      setError("Status could not be updated.");
    } finally {
      setBusy(false);
    }
  }
  if (!item) return <p>{error || "Loading incident…"}</p>;
  return (
    <>
      <Link className="text-link" to="/incidents">
        ← All incidents
      </Link>
      <PageHead
        eyebrow={item.id}
        title={item.title}
        desc={"Reported by " + item.student + " · " + stamp(item.date)}
      >
        <Badge value={item.status} />
      </PageHead>
      {error && <p className="notice">{error}</p>}
      <div className="detail-grid">
        <section className="panel detail">
          <h2>Incident description</h2>
          <p>{item.description}</p>
          <div className="detail-facts">
            <div>
              <small>INCIDENT TYPE</small>
              <p>
                {item.type === "Other"
                  ? item.customType || item.type
                  : item.type}
              </p>
            </div>
            <div>
              <small>SEVERITY</small>
              <p>
                <Badge value={item.severity} />
              </p>
            </div>
          </div>
          <h2>Reported indicators</h2>
          {[
            ["Suspicious URL", item.url],
            ["Domain", item.domain],
            ["IP address", item.ip],
            ["Sender email or phone number", item.sender],
          ]
            .filter((x) => x[1])
            .map(([k, v]) => (
              <div className="fact" key={k}>
                <span>{k}</span>
                <code>{v}</code>
              </div>
            ))}
          <h2>Evidence</h2>
          <p className="muted">
            {item.evidence
              ? `${item.evidence} — ${demo ? "filename retained in demo; file upload requires the backend" : "submitted attachment"}`
              : "No evidence attached."}
          </p>
        </section>
        <section className="panel detail">
          <h2>Investigation</h2>
          <p className="muted">Current status</p>
          <Badge value={item.status} />
          {user.role === "ADMIN" && (
            <label className="field">
              Update status
              <select
                disabled={busy}
                value={item.status}
                onChange={(e) => update(e.target.value)}
              >
                {["Pending", "Investigating", "Resolved"].map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </label>
          )}
          <h2>
            Related reports <span className="count">{related.length}</span>
          </h2>
          {related.map((x) => (
            <Link className="related" key={x.id} to={"/incidents/" + x.id}>
              <Link2 size={16} />
              <span>
                {x.title}
                <small>{x.id}</small>
              </span>
              <ChevronRight size={16} />
            </Link>
          ))}
          {!related.length && (
            <p className="muted">No shared indicators found.</p>
          )}
          <Link className="button secondary" to="/network">
            View incident network <ArrowUpRight size={15} />
          </Link>
        </section>
      </div>
    </>
  );
}
export default Details;
