import React, { useState, useEffect } from "react";
import { Link, NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Network,
  FileText,
  Plus,
  ArrowUpRight,
  LogOut,
  X,
} from "lucide-react";
export default function Sidebar({ mobile, setMobile, items, user, onLogout }) {
  return (
    <aside className={"sidebar " + (mobile ? "opened" : "")}>
      <Link to="/" className="brand">
        <span className="brand-mark"></span>
        Trace<span className="mint">X</span>
      </Link>
      <div className="workspace">
        <span className="campus-icon">C</span>
        <div>
          Campus workspace<small>Student security network</small>
        </div>
        <span className="online" />
      </div>
      <small className="nav-label">WORKSPACE</small>
      <nav onClick={() => setMobile(false)}>
        <NavLink to="/" end>
          <LayoutDashboard size={18} />
          Overview
        </NavLink>
        <NavLink to="/incidents">
          <FileText size={18} />
          Incidents<span className="nav-count">{items.length}</span>
        </NavLink>
        <NavLink to="/network">
          <Network size={18} />
          Incident network
        </NavLink>
        <NavLink to="/report">
          <Plus size={18} />
          Report incident
        </NavLink>
        {user.role === "ADMIN" && <NavLink to="/admin">Admin console</NavLink>}
      </nav>
      <div className="sidebar-bottom">
        <div className="secure-box">
          <strong>A safer campus, together.</strong>
          <p>Every report helps connect the dots.</p>
          <Link to="/report">
            Make a report <ArrowUpRight size={14} />
          </Link>
        </div>
        <button
          className="profile"
          onClick={() => {
            onLogout();
          }}
        >
          <span className="avatar">{user.name.slice(0, 1)}</span>
          <span>
            {user.name}
            <small>
              {user.role === "ADMIN" ? "Administrator" : "Student account"}
            </small>
          </span>
          <LogOut size={16} />
        </button>
      </div>
    </aside>
  );
}
