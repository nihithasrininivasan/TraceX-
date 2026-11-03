import React from "react";
import IncidentList from "./IncidentList";
export default function AdminDashboard({ items }) {
  return <IncidentList items={items} admin />;
}
