
package com.traceroute.incident.controller;

import com.traceroute.incident.entity.IncidentRelationship;
import com.traceroute.incident.service.IncidentService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relationships")
@CrossOrigin(origins = "http://localhost:5173")
public class RelationshipController {

    private final IncidentService incidentService;

    public RelationshipController(
            IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // Get all detected relationships
    @GetMapping
    public List<IncidentRelationship> getAllRelationships() {
        return incidentService.visibleRelationships();
    }

    // Get relationships involving a particular incident
    @GetMapping("/incident/{incidentId}")
    public List<IncidentRelationship> getRelationshipsByIncident(
            @PathVariable Long incidentId) {

        incidentService.getIncidentById(incidentId);
        return incidentService.visibleRelationships().stream().filter(link -> link.getSourceIncidentId().equals(incidentId) || link.getTargetIncidentId().equals(incidentId)).toList();
    }
}
