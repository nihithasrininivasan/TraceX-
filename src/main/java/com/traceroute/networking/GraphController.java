
package com.traceroute.networking;

import com.traceroute.incident.entity.Incident;
import com.traceroute.incident.entity.IncidentRelationship;
import com.traceroute.incident.repository.IncidentRepository;
import com.traceroute.incident.repository.IncidentRelationshipRepository;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/network")
@CrossOrigin(origins = "http://localhost:5173")
public class GraphController {

    private final IncidentRepository incidentRepository;
    private final IncidentRelationshipRepository relationshipRepository;

    public GraphController(
            IncidentRepository incidentRepository,
            IncidentRelationshipRepository relationshipRepository) {

        this.incidentRepository = incidentRepository;
        this.relationshipRepository = relationshipRepository;
    }

    @GetMapping("/graph")
    public Map<String, Object> getIncidentGraph() {

        List<Incident> incidents = incidentRepository.findAll();

        List<IncidentRelationship> relationships =
                relationshipRepository.findAll();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // CREATE REACT FLOW NODES
        for (int i = 0; i < incidents.size(); i++) {

            Incident incident = incidents.get(i);

            Map<String, Object> node = new LinkedHashMap<>();

            node.put("id", String.valueOf(incident.getIncidentId()));

            // Simple grid layout for initial visualization
            Map<String, Object> position = new HashMap<>();
            position.put("x", (i % 4) * 250);
            position.put("y", (i / 4) * 160);

            node.put("position", position);

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("label", "Incident #" + incident.getIncidentId());
            data.put("incidentType", incident.getIncidentType());
            data.put("status", incident.getStatus());
            data.put("domain", incident.getDomain());
            data.put("sampleIp", incident.getSampleIp());
            data.put("description", incident.getDescription());

            node.put("data", data);

            nodes.add(node);
        }

        // CREATE REACT FLOW EDGES
        for (IncidentRelationship relationship : relationships) {

            Map<String, Object> edge = new LinkedHashMap<>();

            edge.put(
                    "id",
                    "relationship-" + relationship.getRelationshipId()
            );

            edge.put(
                    "source",
                    String.valueOf(relationship.getSourceIncidentId())
            );

            edge.put(
                    "target",
                    String.valueOf(relationship.getTargetIncidentId())
            );

            edge.put("label", relationship.getRelationshipType());

            Map<String, Object> edgeData = new LinkedHashMap<>();

            edgeData.put("matchScore", relationship.getMatchScore());
            edgeData.put("relationshipType",
                    relationship.getRelationshipType());
            edgeData.put("matchedIndicator",
                    relationship.getMatchedIndicator());
            edgeData.put("verificationStatus",
                    relationship.getVerificationStatus());

            edge.put("data", edgeData);

            edges.add(edge);
        }

        // FINAL GRAPH RESPONSE
        Map<String, Object> graph = new LinkedHashMap<>();

        graph.put("nodes", nodes);
        graph.put("edges", edges);

        return graph;
    }
}
