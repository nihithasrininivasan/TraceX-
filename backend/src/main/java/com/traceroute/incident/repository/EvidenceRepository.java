package com.traceroute.incident.repository;
import com.traceroute.incident.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByIncidentIncidentId(Long incidentId);
}
