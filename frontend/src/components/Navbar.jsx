import React from "react";
import { Link, NavLink } from "react-router-dom";
import { LogOut, Plus } from "lucide-react";
import { demo } from "../services";

export default function Navbar({ user, onLogout }) {
  return (
    <header className="topbar">
      <Link to="/" className="brand" aria-label="TraceX home">
        Trace<span>X</span>
      </Link>
      <nav className="main-nav" aria-label="Main navigation">
        <NavLink to="/" end>
          Overview
        </NavLink>
        <NavLink to="/incidents">Reports</NavLink>
        <NavLink to="/network">Network</NavLink>
        {user.role === "ADMIN" && <NavLink to="/admin">Admin</NavLink>}
        <NavLink className="nav-report" to="/report">
          <Plus size={13} />
          Report
        </NavLink>
      </nav>
      <div className="account-tools">
        <span className="environment">
          <i />
          {demo ? "Demo" : "Campus API"}
        </span>
        <span className="avatar" title={user.name}>
          {user.name[0]}
        </span>
        <button
          className="icon logout-button"
          onClick={onLogout}
          aria-label="Sign out"
          title="Sign out"
        >
          <LogOut size={16} />
        </button>
      </div>
    </header>
  );
}
