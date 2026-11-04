
package com.traceroute.networking;

import com.traceroute.incident.dto.IncidentResponse;
import com.traceroute.incident.service.IncidentService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IPAnalysisService {

    private final IncidentService incidentService;

    public IPAnalysisService(
            IncidentService incidentService) {

        this.incidentService = incidentService;
    }

    // Find all incidents sharing the given IP address
    public List<IncidentResponse> findRelatedIncidents(String ipAddress) {

        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException(
                    "IP address cannot be empty"
            );
        }

        return incidentService.getAllIncidents().stream().filter(incident -> ipAddress.equals(incident.sampleIp())).toList();
    }

    // Count incidents sharing the same IP
    public int countRelatedIncidents(String ipAddress) {

        return findRelatedIncidents(ipAddress).size();
    }
}
