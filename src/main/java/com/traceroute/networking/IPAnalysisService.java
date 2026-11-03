
package com.traceroute.networking;

import com.traceroute.incident.entity.Incident;
import com.traceroute.incident.repository.IncidentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IPAnalysisService {

    private final IncidentRepository incidentRepository;

    public IPAnalysisService(
            IncidentRepository incidentRepository) {

        this.incidentRepository = incidentRepository;
    }

    // Find all incidents sharing the given IP address
    public List<Incident> findRelatedIncidents(String ipAddress) {

        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException(
                    "IP address cannot be empty"
            );
        }

        return incidentRepository.findBySampleIp(ipAddress);
    }

    // Count incidents sharing the same IP
    public int countRelatedIncidents(String ipAddress) {

        return findRelatedIncidents(ipAddress).size();
    }
}
