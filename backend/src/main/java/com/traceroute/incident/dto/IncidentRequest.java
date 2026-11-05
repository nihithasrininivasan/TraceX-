
package com.traceroute.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import com.traceroute.validation.ContactFormats;

public class IncidentRequest {

    @NotBlank(message = "Incident type is required")
    @Size(max = 100)
    private String incidentType;

    @NotBlank(message = "Description is required")
    @Size(min = 15, max = 1000)
    private String description;

    @Size(max = 255)
    private String suspiciousUrl;

    @Size(max = 254)
    private String senderEmail;

    @AssertTrue(message = "Enter a valid sender email or phone number (10 digits, or + country code and 8–15 digits)")
    public boolean isSenderFormatValid() { return ContactFormats.sender(senderEmail); }

    @Size(max = 140) private String title;
    @Size(max = 20) private String severity;
    @Size(max = 100) private String customType;
    @Size(max = 45) private String sampleIp;
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getSeverity() { return severity; }
    public void setSeverity(String value) { severity = value; }
    public String getCustomType() { return customType; }
    public void setCustomType(String value) { customType = value; }
    public String getSampleIp() { return sampleIp; }
    public void setSampleIp(String value) { sampleIp = value; }

    // Getters and Setters

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuspiciousUrl() {
        return suspiciousUrl;
    }

    public void setSuspiciousUrl(String suspiciousUrl) {
        this.suspiciousUrl = suspiciousUrl;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail == null ? null : senderEmail.strip();
    }
}
