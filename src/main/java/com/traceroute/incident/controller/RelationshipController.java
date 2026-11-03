
package com.traceroute.incident.controller;

import com.traceroute.incident.entity.IncidentRelationship;
import com.traceroute.incident.repository.IncidentRelationshipRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relationships")
@CrossOrigin(origins = "http://localhost:5173")
public class RelationshipController {

    private final IncidentRelationshipRepository relationshipRepository;

    public RelationshipController(
            IncidentRelationshipRepository relationshipRepository) {
        this.relationshipRepository = relationshipRepository;
    }

    // Get all detected relationships
    @GetMapping
    public List<IncidentRelationship> getAllRelationships() {
        return relationshipRepository.findAll();
    }

    // Get relationships involving a particular incident
    @GetMapping("/incident/{incidentId}")
    public List<IncidentRelationship> getRelationshipsByIncident(
            @PathVariable Long incidentId) {

        return relationshipRepository
                .findBySourceIncidentIdOrTargetIncidentId(
                        incidentId, incidentId);
    }
}
