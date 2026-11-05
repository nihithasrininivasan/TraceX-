import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FileText, ArrowRight, Globe, User, X } from "lucide-react";
import { service, demo } from "../services";
import { isValidEmail } from "../utils/validation";
function Auth({ enter, register = false }) {
  const nav = useNavigate();
  const [err, setErr] = useState(""),
    [busy, setBusy] = useState(false);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setErr("");
    try {
      const body = Object.fromEntries(new FormData(e.currentTarget));
      body.email = body.email.trim();
      if (!isValidEmail(body.email))
        throw new Error("Enter a complete email address, e.g. you@campus.edu.");
      if (register) {
        if (body.password !== body.confirmPassword)
          throw new Error("Passwords do not match.");
        const { confirmPassword, ...details } = body;
        await service.register(details);
        nav("/login?registered=1");
      } else {
        const r = await service.login(body);
        enter(r.user);
      }
    } catch (e) {
      const response = e.response?.data;
      setErr(
        (response?.errors && Object.values(response.errors).join(" ")) ||
          response?.message ||
          e.message,
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-page">
      <div className="auth-story">
        <Link to="/" className="brand">
          Trace<span className="mint">X</span>
        </Link>
        <small className="eyebrow">CAMPUS CYBER INCIDENT NETWORK</small>
        <h1>
          Every signal
          <br />
          has a story<span className="mint">.</span>
        </h1>
        <p>
          Connect the dots. Protect your community.
          <br />A shared view of campus cybersecurity.
        </p>
        <div className="auth-network">
          <div>
            <User /> Students
          </div>
          <span>─────────</span>
          <div>
            <FileText /> Reports
          </div>
          <span>─────────</span>
          <div>
            <Globe /> Connections
          </div>
        </div>
        <small>BETTER VISIBILITY. STRONGER COMMUNITY.</small>
      </div>
      <section className="auth-card">
        <small className="eyebrow">WELCOME TO TRACEX</small>
        <h2>
          {register ? "Join your campus network" : "Your campus. Your watch."}
        </h2>
        <p>
          {register
            ? "Create your student account."
            : "Sign in to your security workspace."}
        </p>
        <form onSubmit={submit}>
          {register && (
            <label className="field">
              Full name
              <input name="name" required maxLength={100} autoComplete="name" />
            </label>
          )}
          <label className="field">
            Campus email
            <input
              name="email"
              type="email"
              maxLength={254}
              required
              autoComplete="email"
              placeholder="you@campus.edu"
            />
          </label>
          <label className="field">
            Password
            <input
              name="password"
              type="password"
              minLength={register ? 8 : 1}
              maxLength={72}
              pattern={register ? "[\\x20-\\x7E]{8,72}" : "[\\x20-\\x7E]{1,72}"}
              title="Use printable ASCII characters; maximum 72 characters."
              required
              autoComplete={register ? "new-password" : "current-password"}
            />
          </label>
          {register && (
            <label className="field">
              Confirm password
              <input
                name="confirmPassword"
                type="password"
                minLength={8}
                maxLength={72}
                required
                autoComplete="new-password"
              />
            </label>
          )}
          {!register &&
            new URLSearchParams(location.hash.split("?")[1] || "").has(
              "registered",
            ) && (
              <p className="success-notice" role="status">
                Account created. Sign in to continue.
              </p>
            )}
          {err && (
            <p className="notice" role="alert">
              {err}
            </p>
          )}
          <button disabled={busy} className="button primary">
            {busy ? "Please wait…" : register ? "Create account" : "Sign in"}
            <ArrowRight size={16} />
          </button>
        </form>
        <p>
          {register ? "Already have an account?" : "New to TraceX?"}{" "}
          <button
            className="text-button"
            onClick={() => {
              nav(register ? "/login" : "/register");
              setErr("");
            }}
          >
            {register ? "Sign in" : "Create account"}
          </button>
        </p>
        {demo && (
          <div className="demo-access">
            <small>EXPLORE THE INTERACTIVE DEMO</small>
            <div>
              <button
                className="button secondary"
                onClick={() => enter({ name: "Aarav Mehta", role: "STUDENT" })}
              >
                Student access <ArrowRight size={14} />
              </button>
              <button
                className="button secondary"
                onClick={() => enter({ name: "Campus Admin", role: "ADMIN" })}
              >
                Admin access
              </button>
            </div>
            <p>Sample data. Demo access does not create an account.</p>
          </div>
        )}
      </section>
    </div>
  );
}
export default Auth;
