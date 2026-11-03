import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import {
  FileText,
  ArrowRight,
  Globe,
  Link2,
  Server,
  User,
  X,
} from "lucide-react";
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
} from "@xyflow/react";
import { service, demo } from "../services";
import Badge from "./Badge";
import { makeGraph } from "../utils/graph";
const icons = {
  student: User,
  incident: FileText,
  domain: Globe,
  ip: Server,
  url: Link2,
};
function GraphNode({ data }) {
  const Icon = icons[data.kind] || FileText;
  return (
    <div className={`graphnode ${data.kind}`}>
      <Handle type="target" position={Position.Left} />
      <span>
        <Icon size={17} />
      </span>
      <div>
        <small>{data.kind.toUpperCase()}</small>
        <strong>{data.label}</strong>
      </div>
      <Handle type="source" position={Position.Right} />
    </div>
  );
}
const nodeTypes = { entity: GraphNode };
function NetworkView({ items, compact = false }) {
  const [selected, setSelected] = useState(null),
    [kind, setKind] = useState("All entities"),
    [remote, setRemote] = useState(null),
    [err, setErr] = useState("");
  useEffect(() => {
    let active = true;
    setErr("");
    if (!demo)
      service
        .graph()
        .then((graph) => {
          if (active) setRemote(graph);
        })
        .catch(
          () =>
            active &&
            setErr(
              "Graph API unavailable. Showing connections derived from loaded reports.",
            ),
        );
    return () => {
      active = false;
    };
  }, [items]);
  let graph = remote || makeGraph(items);
  if (kind !== "All entities") {
    const ids = new Set(
      graph.nodes
        .filter((n) => n.data.kind === kind || n.data.kind === "incident")
        .map((n) => n.id),
    );
    graph = {
      nodes: graph.nodes.filter((n) => ids.has(n.id)),
      edges: graph.edges.filter((e) => ids.has(e.source) && ids.has(e.target)),
    };
  }
  return (
    <div className={"network-wrap " + (compact ? "compact" : "")}>
      <div className="graph-toolbar">
        <div className="live-dot" />{" "}
        <span>
          {graph.nodes.length} nodes <b>·</b> {graph.edges.length} connections
        </span>
        <select
          aria-label="Filter graph entities"
          value={kind}
          onChange={(e) => setKind(e.target.value)}
        >
          {["All entities", "student", "domain", "url", "ip"].map((x) => (
            <option key={x}>{x}</option>
          ))}
        </select>
      </div>
      {err && <p className="notice">{err}</p>}
      <div className="graph-canvas">
        <ReactFlow
          key={kind + graph.nodes.map((node) => node.id).join("|")}
          nodes={graph.nodes}
          edges={graph.edges}
          nodeTypes={nodeTypes}
          fitView
          fitViewOptions={{ padding: 0.18 }}
          minZoom={0.15}
          maxZoom={2}
          nodesDraggable={false}
          nodesConnectable={false}
          onNodeClick={(_, n) => setSelected(n.data)}
          proOptions={{ hideAttribution: true }}
        >
          <Background color="#354159" gap={26} size={0.7} />
          <Controls showInteractive={false} />
          {!compact && <MiniMap nodeColor="#526d98" maskColor="#111216a0" />}
        </ReactFlow>
        {!graph.nodes.length && (
          <div className="graph-empty">
            No reports to visualize yet. Submit an incident to build the
            network.
          </div>
        )}
        <div className="graph-legend">
          {["student", "incident", "domain", "ip"].map((k) => (
            <span key={k}>
              <i className={k} />
              {k === "ip" ? "IP address" : k}
            </span>
          ))}
        </div>
        {selected && (
          <aside className="node-inspector">
            <button
              className="icon close"
              onClick={() => setSelected(null)}
              aria-label="Close node details"
            >
              <X size={16} />
            </button>
            <small>ENTITY DETAILS</small>
            <h3>{selected.label}</h3>
            <Badge value={selected.kind} />
            <p>{selected.detail}</p>
            {selected.incident && (
              <Link
                className="text-link"
                to={"/incidents/" + selected.incident.id}
              >
                Open incident <ArrowRight size={14} />
              </Link>
            )}
          </aside>
        )}
      </div>
    </div>
  );
}
export default NetworkView;
