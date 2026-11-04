
package com.traceroute.incident.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.traceroute.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incidentId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(length = 140)
    private String title;
    @Column(length = 20)
    private String severity;
    @Column(length = 100)
    private String customType;
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getCustomType() { return customType; }
    public void setCustomType(String customType) { this.customType = customType; }

    @Column(nullable = false)
    private String incidentType;

    @Column(length = 1000)
    private String description;

    private String suspiciousUrl;

    private String domain;

    private String senderEmail;

    private String sampleIp;

    private String status;

    private LocalDateTime reportedAt;

    // Default constructor
    public Incident() {
    }

    // Automatically set date before saving
    @PrePersist
    public void onCreate() {
        reportedAt = LocalDateTime.now(ZoneOffset.UTC);

        if (status == null) {
            status = "PENDING";
        }
    }

    // Getters and Setters

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuspiciousUrl() {
        return suspiciousUrl;
    }

    public void setSuspiciousUrl(String suspiciousUrl) {
        this.suspiciousUrl = suspiciousUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSampleIp() {
        return sampleIp;
    }

    public void setSampleIp(String sampleIp) {
        this.sampleIp = sampleIp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
