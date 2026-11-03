import React, { useState, useEffect } from "react";
import { Navigate, Routes, Route, useNavigate } from "react-router-dom";
import { readUser } from "./utils/session";
import Navbar from "./components/Navbar";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import IncidentList from "./pages/IncidentList";
import Details from "./pages/IncidentDetails";
import Report from "./pages/ReportIncident";
import IncidentNetwork from "./pages/IncidentNetwork";
import AdminDashboard from "./pages/AdminDashboard";
import { service, demo, clearCredentials } from "./services";
function App() {
  const [user, setUser] = useState(() => (demo ? readUser() : null));
  const [items, setItems] = useState([]),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true);
  const nav = useNavigate();
  async function refresh() {
    try {
      setItems(await service.list());
      setError("");
    } catch {
      setError("Unable to load incidents. Check the backend connection.");
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    if (user) refresh();
  }, [user]);
  function enter(u) {
    if (demo) sessionStorage.setItem("tracex-user", JSON.stringify(u));
    setUser(u);
    nav("/");
  }
  function logout() {
    clearCredentials();
    sessionStorage.removeItem("tracex-user");
    sessionStorage.removeItem("tracex-token");
    setUser(null);
    setItems([]);
    setLoading(true);
    nav("/login");
  }
  useEffect(() => {
    const expire = () => {
      logout();
    };
    window.addEventListener("tracex-session-expired", expire);
    return () => window.removeEventListener("tracex-session-expired", expire);
  }, []);
  if (!user)
    return (
      <Routes>
        <Route path="/register" element={<Register enter={enter} />} />
        <Route path="/login" element={<Login enter={enter} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  return (
    <div className="app">
      <main>
        <Navbar user={user} onLogout={logout} />
        <div className="content">
          {error && (
            <div className="notice">
              {error}
              <button onClick={refresh}>Retry</button>
            </div>
          )}
          {loading ? (
            <p>Loading campus reports…</p>
          ) : (
            <Routes>
              <Route path="/" element={<Dashboard items={items} />} />
              <Route
                path="/network"
                element={<IncidentNetwork items={items} />}
              />
              <Route
                path="/incidents"
                element={<IncidentList items={items} />}
              />
              <Route
                path="/incidents/:id"
                element={<Details user={user} refresh={refresh} />}
              />
              <Route
                path="/report"
                element={<Report user={user} refresh={refresh} />}
              />
              <Route
                path="/admin"
                element={
                  user.role === "ADMIN" ? (
                    <AdminDashboard items={items} />
                  ) : (
                    <Navigate to="/" />
                  )
                }
              />
              <Route path="*" element={<Navigate to="/" />} />
            </Routes>
          )}
          <footer>
            <span>
              TraceX <b> / </b> Campus security, connected.
            </span>
            <span>
              <i className="live-dot" />{" "}
              {demo
                ? "Sample data · stored on this browser"
                : "Connected to campus API"}
            </span>
          </footer>
        </div>
      </main>
    </div>
  );
}
export default App;
