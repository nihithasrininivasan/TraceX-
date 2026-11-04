
package com.traceroute.incident.repository;

import com.traceroute.incident.entity.IncidentRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRelationshipRepository
        extends JpaRepository<IncidentRelationship, Long> {

    List<IncidentRelationship> findBySourceIncidentId(Long incidentId);

    List<IncidentRelationship> findByTargetIncidentId(Long incidentId);

    List<IncidentRelationship> findBySourceIncidentIdOrTargetIncidentId(
            Long sourceId, Long targetId);

    boolean existsBySourceIncidentIdAndTargetIncidentIdAndRelationshipType(
            Long sourceId, Long targetId, String relationshipType);
}
