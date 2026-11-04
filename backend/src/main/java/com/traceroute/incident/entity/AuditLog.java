package com.traceroute.incident.entity;

import com.traceroute.entity.User;
import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long logId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private LocalDateTime createdAt;
    protected AuditLog() {}
    public AuditLog(User user, String action) { this.user=user; this.action=action; this.createdAt=LocalDateTime.now(ZoneOffset.UTC); }
}
