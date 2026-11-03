
package com.traceroute.incident.service;

import com.traceroute.incident.dto.IncidentRequest;
import com.traceroute.incident.entity.Incident;
import com.traceroute.incident.repository.IncidentRepository;
import com.traceroute.networking.DomainExtractor;
import com.traceroute.networking.DNSLookupService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final DomainExtractor domainExtractor;
    private final DNSLookupService dnsLookupService;
    private final RelationshipService relationshipService;

    public IncidentService(
            IncidentRepository incidentRepository,
            DomainExtractor domainExtractor,
            DNSLookupService dnsLookupService,
            RelationshipService relationshipService) {

        this.incidentRepository = incidentRepository;
        this.domainExtractor = domainExtractor;
        this.dnsLookupService = dnsLookupService;
        this.relationshipService = relationshipService;
    }

    // CREATE INCIDENT
    public Incident createIncident(IncidentRequest request) {

        Incident incident = new Incident();

        incident.setIncidentType(request.getIncidentType());
        incident.setDescription(request.getDescription());
        incident.setSuspiciousUrl(request.getSuspiciousUrl());
        incident.setSenderEmail(request.getSenderEmail());

        // Extract domain from suspicious URL
        String domain =
                domainExtractor.extractDomain(request.getSuspiciousUrl());

        incident.setDomain(domain);

        // Perform sample DNS lookup
        String ipAddress = dnsLookupService.lookupIP(domain);

        if (!ipAddress.equals("No sample DNS record found")
                && !ipAddress.equals("Invalid domain")) {

            incident.setSampleIp(ipAddress);
        }

        incident.setStatus("PENDING");

        // Save incident first to generate its ID
        Incident savedIncident = incidentRepository.save(incident);

        // Automatically detect relationships with existing incidents
        relationshipService.detectRelationships(savedIncident);

        return savedIncident;
    }

    // GET ALL INCIDENTS
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    // GET INCIDENT BY ID
    public Incident getIncidentById(Long id) {

        return incidentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Incident not found with ID: " + id
                        ));
    }
}
