
package com.traceroute.incident.repository;

import com.traceroute.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository
        extends JpaRepository<Incident, Long> {

    // Find incidents having the same IP address
    List<Incident> findBySampleIp(String sampleIp);

}
