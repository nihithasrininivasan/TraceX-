
package com.traceroute.incident.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_relationships")
public class IncidentRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long relationshipId;

    @Column(nullable = false)
    private Long sourceIncidentId;

    @Column(nullable = false)
    private Long targetIncidentId;

    @Column(nullable = false)
    private String relationshipType;

    private double matchScore;

    private String verificationStatus;

    private String matchedIndicator;

    private LocalDateTime createdAt;

    public IncidentRelationship() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

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
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
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
