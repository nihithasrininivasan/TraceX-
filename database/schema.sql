-- REFERENCE ONLY. Not on the application classpath and never auto-executed.
-- Create traceroute_db separately; Hibernate ddl-auto=update owns local schema creation.
-- Only users is currently implemented. Remaining tables are teammate 2's proposed contract.
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
);

CREATE TABLE incidents (
    incident_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    incident_type VARCHAR(100) NOT NULL,
    description TEXT,
    suspicious_url TEXT,
    domain VARCHAR(253),
    sender_email VARCHAR(254),
    sample_ip VARCHAR(45),
    status VARCHAR(30) NOT NULL,
    reported_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_incidents_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_incidents_user (user_id)
);

CREATE TABLE evidence (
    evidence_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    evidence_type VARCHAR(100) NOT NULL,
    evidence_value TEXT NOT NULL,
    CONSTRAINT fk_evidence_incident FOREIGN KEY (incident_id) REFERENCES incidents(incident_id)
);

CREATE TABLE incident_relationships (
    relationship_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_incident_id BIGINT NOT NULL,
    target_incident_id BIGINT NOT NULL,
    relationship_type VARCHAR(100) NOT NULL,
    matching_score DECIMAL(5,4) NOT NULL,
    CONSTRAINT fk_relationship_source FOREIGN KEY (source_incident_id) REFERENCES incidents(incident_id),
    CONSTRAINT fk_relationship_target FOREIGN KEY (target_incident_id) REFERENCES incidents(incident_id),
    CONSTRAINT ck_matching_score CHECK (matching_score BETWEEN 0 AND 1)
);

CREATE TABLE audit_logs (
    log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);
