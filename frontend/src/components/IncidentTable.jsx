import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { FileText, ArrowUpRight } from "lucide-react";
import Badge from "./Badge";
import { stamp } from "../utils/format";
function IncidentTable({ items }) {
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>INCIDENT</th>
            <th>TYPE</th>
            <th>REPORTED</th>
            <th>SEVERITY</th>
            <th>STATUS</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {items.map((x) => (
            <tr key={x.id}>
              <td>
                <Link className="incident-title" to={"/incidents/" + x.id}>
                  <span className="file-icon">
                    <FileText size={17} />
                  </span>
                  <span>
                    {x.title}
                    <small>
                      {x.id} <b>·</b> {x.student}
                    </small>
                  </span>
                </Link>
              </td>
              <td>{x.type === "Other" ? x.customType || x.type : x.type}</td>
              <td>{stamp(x.date)}</td>
              <td>
                <span className={"severity " + x.severity.toLowerCase()}>
                  <i />
                  {x.severity}
                </span>
              </td>
              <td>
                <Badge value={x.status} />
              </td>
              <td>
                <Link to={"/incidents/" + x.id} aria-label={"Open " + x.id}>
                  <ArrowUpRight size={16} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!items.length && (
        <div className="empty">No incidents match your filters.</div>
      )}
    </div>
  );
}
export default IncidentTable;
