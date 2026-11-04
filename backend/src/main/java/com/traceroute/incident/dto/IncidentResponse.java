package com.traceroute.incident.dto;
import com.traceroute.incident.entity.Incident;
import java.time.LocalDateTime;
import java.util.List;

public record IncidentResponse(Long incidentId, String incidentType, String title, String description,
        String suspiciousUrl, String domain, String senderEmail, String sampleIp, String status,
        LocalDateTime reportedAt, String severity, String customType, Reporter user, List<EvidenceSummary> evidenceFiles) {
    public record Reporter(Long id, String name) {}
    public record EvidenceSummary(Long id, String filename, String contentType) {}
    public static IncidentResponse from(Incident value, List<EvidenceSummary> evidence) {
        var user = value.getUser();
        return new IncidentResponse(value.getIncidentId(), value.getIncidentType(), value.getTitle(), value.getDescription(),
            value.getSuspiciousUrl(), value.getDomain(), value.getSenderEmail(), value.getSampleIp(), value.getStatus(),
            value.getReportedAt(), value.getSeverity(), value.getCustomType(), user == null ? null : new Reporter(user.getId(), user.getName()), evidence);
    }
}
