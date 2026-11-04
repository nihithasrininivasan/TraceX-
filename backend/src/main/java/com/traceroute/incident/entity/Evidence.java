package com.traceroute.incident.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evidence")
public class Evidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long evidenceId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "incident_id", nullable = false) private Incident incident;
    @Column(name = "evidence_type", nullable = false, length = 100) private String contentType;
    @Column(name = "evidence_value", nullable = false, columnDefinition = "TEXT") private String filename;
    @Lob @Column(nullable = false, columnDefinition = "LONGBLOB") private byte[] content;
    protected Evidence() {}
    public Evidence(Incident incident, String filename, String contentType, byte[] content) {
        this.incident = incident; this.filename = filename; this.contentType = contentType; this.content = content;
    }
    public Long getEvidenceId() { return evidenceId; }
    public Incident getIncident() { return incident; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public byte[] getContent() { return content; }
}
