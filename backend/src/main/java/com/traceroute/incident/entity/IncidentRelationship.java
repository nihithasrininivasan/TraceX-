
package com.traceroute.incident.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;

@Entity
@Table(name = "incident_relationships")
public class IncidentRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long relationshipId;

    @Column(name = "source_incident_id", nullable = false)
    private Long sourceIncidentId;

    @Column(name = "target_incident_id", nullable = false)
    private Long targetIncidentId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_incident_id",insertable=false,updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore private Incident source;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="target_incident_id",insertable=false,updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore private Incident target;

    @Column(nullable = false)
    private String relationshipType;

    @Column(name="matching_score", precision=5, scale=4)
    private BigDecimal matchScore;

    private String verificationStatus;

    private String matchedIndicator;

    private LocalDateTime createdAt;

    public IncidentRelationship() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);

        if (verificationStatus == null) {
            verificationStatus = "PENDING";
        }
    }

    public Long getRelationshipId() {
        return relationshipId;
    }

    public void setRelationshipId(Long relationshipId) {
        this.relationshipId = relationshipId;
    }

    public Long getSourceIncidentId() {
        return sourceIncidentId;
    }

    public void setSourceIncidentId(Long sourceIncidentId) {
        this.sourceIncidentId = sourceIncidentId;
    }

    public Long getTargetIncidentId() {
        return targetIncidentId;
    }

    public void setTargetIncidentId(Long targetIncidentId) {
        this.targetIncidentId = targetIncidentId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public double getMatchScore() {
        return matchScore == null ? 0 : matchScore.doubleValue();
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = BigDecimal.valueOf(matchScore);
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getMatchedIndicator() {
        return matchedIndicator;
    }

    public void setMatchedIndicator(String matchedIndicator) {
        this.matchedIndicator = matchedIndicator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
