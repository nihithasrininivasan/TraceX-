import { validateReport } from "../utils/validation";
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, Upload } from "lucide-react";
import PageHead from "../components/PageHead";
import { service, demo } from "../services";
function Report({ user, refresh }) {
  const nav = useNavigate(),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [type, setType] = useState("Phishing"),
    [file, setFile] = useState(null);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const body = Object.fromEntries(new FormData(e.currentTarget));
    if (body.customType) body.customType = body.customType.trim();
    const validation = validateReport({ ...body, evidence: file });
    if (validation) {
      setError(validation);
      setBusy(false);
      return;
    }
    try {
      const x = await service.create({
        ...body,
        evidence: file,
        student: user.name,
      });
      await refresh();
      nav("/incidents/" + x.id);
    } catch {
      setError(
        "Report could not be submitted. Please check your connection and try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHead
        eyebrow="SPEAK UP. STAY SECURE."
        title="Report an incident"
        desc="Share what happened. Help your campus connect the dots."
      />
      <form className="panel report-form" onSubmit={submit}>
        <div className="form-heading">
          <div>
            <h2>Incident information</h2>
            <p>
              Include the details you know. Avoid sharing passwords or one-time
              codes.
            </p>
          </div>
        </div>
        <label className="field">
          Incident title
          <input
            name="title"
            required
            maxLength={140}
            placeholder="A short summary of what happened"
          />
        </label>
        <div className="form-row">
          <label className="field">
            Incident type
            <select
              name="type"
              value={type}
              onChange={(event) => setType(event.target.value)}
            >
              {[
                "Phishing",
                "Suspicious URL",
                "Fake phone call",
                "Compromised account",
                "Spam",
                "Other",
              ].map((x) => (
                <option key={x}>{x}</option>
              ))}
            </select>
          </label>
          <label className="field">
            Severity
            <select name="severity">
              {["Low", "Medium", "High"].map((x) => (
                <option key={x}>{x}</option>
              ))}
            </select>
          </label>
        </div>
        {type === "Other" && (
          <label className="field">
            Specify incident type
            <input
              name="customType"
              required
              maxLength={100}
              placeholder="e.g. SMS phishing, QR code scam, impersonation"
            />
          </label>
        )}
        <label className="field">
          Description
          <textarea
            name="description"
            required
            minLength={15}
            rows={5}
            placeholder="What did you notice? When and where did it happen?"
          />
        </label>
        <div className="form-row">
          <label className="field">
            Suspicious URL <small>optional</small>
            <input type="url" name="url" placeholder="https://example.com" />
          </label>
          <label className="field">
            Sender email or phone number <small>optional</small>
            <input
              type="text"
              name="sender"
              placeholder="sender@example.com or +91 98765 43210"
            />
          </label>
        </div>
        <label className="field">
          IP address <small>optional</small>
          <input
            name="ip"
            placeholder="e.g. 185.234.72.19"
            pattern="([0-9]{1,3}\.){3}[0-9]{1,3}"
          />
        </label>
        <label className="upload">
          <Upload size={22} />
          <strong>{file ? file.name : "Attach supporting evidence"}</strong>
          <span>PNG, JPG, or PDF · up to 5 MB</span>
          <input
            aria-label="Evidence attachment"
            type="file"
            accept="image/png,image/jpeg,application/pdf"
            onChange={(e) => {
              const f = e.target.files[0];
              if (f && f.size > 5 * 1024 * 1024) {
                setError("Choose a file smaller than 5 MB.");
                e.target.value = "";
                setFile(null);
              } else {
                setError("");
                setFile(f);
              }
            }}
          />
        </label>
        {error && <p className="notice">{error}</p>}
        <div className="form-actions">
          <span>
            {demo
              ? "Demo report saved in this browser"
              : "Submitted to campus security"}
          </span>
          <button disabled={busy} className="button primary">
            {busy ? "Submitting…" : "Submit report"}
            <ArrowRight size={16} />
          </button>
        </div>
      </form>
    </>
  );
}
export default Report;
