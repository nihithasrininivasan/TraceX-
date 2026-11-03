
package com.traceroute.incident.controller;

import com.traceroute.incident.dto.IncidentRequest;
import com.traceroute.incident.entity.Incident;
import com.traceroute.incident.service.IncidentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "http://localhost:5173")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // POST - Create incident
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Incident createIncident(
            @Valid @RequestBody IncidentRequest request) {

        return incidentService.createIncident(request);
    }

    // GET - Retrieve all incidents
    @GetMapping
    public List<Incident> getAllIncidents() {

        return incidentService.getAllIncidents();
    }

    // GET - Retrieve incident by ID
    @GetMapping("/{id}")
    public Incident getIncidentById(@PathVariable Long id) {

        return incidentService.getIncidentById(id);
    }
}

