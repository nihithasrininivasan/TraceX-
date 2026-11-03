# Entity contract and ownership

## Teammate 1 — implemented

`com.traceroute.entity.User` maps to `users`: `id BIGINT` identity primary key, `name VARCHAR(100) NOT NULL`, `email VARCHAR(254) NOT NULL UNIQUE`, `password_hash VARCHAR(60) NOT NULL`, `role VARCHAR(20) NOT NULL` (`STUDENT` or `ADMIN`), `created_at DATETIME(6) NOT NULL`. Email is normalized to lowercase before persistence by registration. `createdAt` uses UTC, assigned on insert. Public responses use UserResponse, never User or its hash. No role ordinal storage.

## Teammate 2 — reserved, not implemented

Owns Incident.java, Evidence.java, IncidentRelationship.java, incident repositories/controllers/services, networking demos and graph logic. The following MySQL design is a reference for integration; keep FK column names consistent. See `database/schema.sql` for proposed lengths, indexes and foreign keys. No cascading deletes are assumed.

| Table | Columns |
| --- | --- |
| incidents | incident_id, user_id, incident_type, description, suspicious_url, domain, sender_email, sample_ip, status, reported_at |
| evidence | evidence_id, incident_id, evidence_type, evidence_value |
| incident_relationships | relationship_id, source_incident_id, target_incident_id, relationship_type, matching_score |
| audit_logs | log_id, user_id, action, created_at |

Required Incident.java fields (to be created by teammate 2):

```java
@Entity
@Table(name = "incidents")
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    Long incidentId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    String incidentType;
    String description;
    String suspiciousUrl;
    String domain;
    String senderEmail;
    String sampleIp;
    String status;
    LocalDateTime reportedAt;
}
```

Package: `com.traceroute.entity`; imports: jakarta.persistence annotations and java.time.LocalDateTime. Add explicit `@Column` mappings and lengths matching the reference schema, plus constructors/accessors and lifecycle/validation rules. Spring's naming strategy maps camelCase to snake_case. Reference `users.id` for `user_id`. Prefer DTOs to serializing entity graphs, and ensure only authorized users can access incidents. Use UTC consistently. Incident types, evidence types, statuses, relationship types, matching-score scale and audit ownership are decisions to finalize with teammate 2; the reference proposes DECIMAL(5,4) scores in [0,1].

Teammate 3 owns the frontend. No React files or UI are implemented here.
