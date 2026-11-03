
package com.traceroute.incident.service;

import com.traceroute.incident.entity.Incident;
import com.traceroute.incident.entity.IncidentRelationship;
import com.traceroute.incident.repository.IncidentRepository;
import com.traceroute.incident.repository.IncidentRelationshipRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RelationshipService {

    private final IncidentRepository incidentRepository;
    private final IncidentRelationshipRepository relationshipRepository;

    public RelationshipService(
            IncidentRepository incidentRepository,
            IncidentRelationshipRepository relationshipRepository) {

        this.incidentRepository = incidentRepository;
        this.relationshipRepository = relationshipRepository;
    }

    public void detectRelationships(Incident newIncident) {

        List<Incident> existingIncidents = incidentRepository.findAll();

        for (Incident existing : existingIncidents) {

            // Avoid comparing an incident with itself
            if (existing.getIncidentId().equals(newIncident.getIncidentId())) {
                continue;
            }

            boolean sameDomain = isSame(
                    existing.getDomain(),
                    newIncident.getDomain()
            );

            boolean sameIP = isSame(
                    existing.getSampleIp(),
                    newIncident.getSampleIp()
            );

            // Calculate relationship score
            double score = 0;
            StringBuilder matchedIndicator = new StringBuilder();

            if (sameDomain) {
                score += 60;
                matchedIndicator.append("DOMAIN: ")
                        .append(newIncident.getDomain());
            }

            if (sameIP) {
                score += 40;

                if (matchedIndicator.length() > 0) {
                    matchedIndicator.append("; ");
                }

                matchedIndicator.append("IP: ")
                        .append(newIncident.getSampleIp());
            }

            // No shared indicators, so no relationship
            if (score == 0) {
                continue;
            }

            // Keep older incident as source and newer as target
            Long sourceId = Math.min(
                    existing.getIncidentId(),
                    newIncident.getIncidentId()
            );

            Long targetId = Math.max(
                    existing.getIncidentId(),
                    newIncident.getIncidentId()
            );

            String relationshipType;

            if (sameDomain && sameIP) {
                relationshipType = "DOMAIN_AND_IP_MATCH";
            } else if (sameDomain) {
                relationshipType = "DOMAIN_MATCH";
            } else {
                relationshipType = "IP_MATCH";
            }

            // Avoid duplicate relationships
            boolean alreadyExists =
                    relationshipRepository
                            .existsBySourceIncidentIdAndTargetIncidentIdAndRelationshipType(
                                    sourceId,
                                    targetId,
                                    relationshipType
                            );

            if (alreadyExists) {
                continue;
            }

            // Create and save relationship
            IncidentRelationship relationship =
                    new IncidentRelationship();

            relationship.setSourceIncidentId(sourceId);
            relationship.setTargetIncidentId(targetId);
            relationship.setRelationshipType(relationshipType);
            relationship.setMatchScore(score);
            relationship.setMatchedIndicator(
                    matchedIndicator.toString()
            );
            relationship.setVerificationStatus("PENDING");

            relationshipRepository.save(relationship);
        }
    }

    private boolean isSame(String first, String second) {

        if (first == null || second == null) {
            return false;
        }

        if (first.isBlank() || second.isBlank()) {
            return false;
        }

        return first.trim().equalsIgnoreCase(second.trim());
    }
}

