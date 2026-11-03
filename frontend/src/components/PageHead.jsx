import React, { useState, useEffect } from "react";

function PageHead({ eyebrow, title, desc, children }) {
  return (
    <div className="pagehead">
      <div>
        <small className="eyebrow">{eyebrow}</small>
        <h1>
          {title}
          <span className="title-dot">.</span>
        </h1>
        <p>{desc}</p>
      </div>
      {children}
    </div>
  );
}
export default PageHead;
