import React, { useState, useEffect } from "react";

import PageHead from "../components/PageHead";
import NetworkView from "../components/NetworkGraph";
export default function IncidentNetwork({ items }) {
  return (
    <>
      <PageHead
        eyebrow="CONNECTED INTELLIGENCE"
        title="Incident network"
        desc="Follow the connections. Uncover the bigger picture."
      />
      <div className="panel">
        <NetworkView items={items} />
      </div>
      <p className="footnote">
        Shared indicators suggest a relationship; they do not confirm the same
        attacker. Click a node to inspect it.
      </p>
    </>
  );
}
