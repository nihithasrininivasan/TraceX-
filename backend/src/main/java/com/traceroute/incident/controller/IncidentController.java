package com.traceroute.incident.controller;

import com.traceroute.incident.dto.*;
import com.traceroute.incident.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.nio.charset.StandardCharsets;

@RestController @RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidents;
    public IncidentController(IncidentService incidents) { this.incidents=incidents; }
    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse create(@Valid @RequestBody IncidentRequest request) { return incidents.createIncident(request,null); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createMultipart(@Valid @RequestPart("incident") IncidentRequest request,
            @RequestPart(value="evidence",required=false) MultipartFile evidence) { return incidents.createIncident(request,evidence); }
    @GetMapping public List<IncidentResponse> list() { return incidents.getAllIncidents(); }
    @GetMapping("/{id}") public IncidentResponse detail(@PathVariable Long id) { return incidents.getIncidentById(id); }
    @GetMapping("/{id}/related") public List<IncidentResponse> related(@PathVariable Long id) { return incidents.related(id); }
    public record StatusRequest(String status) {}
    @PutMapping("/{id}/status") public IncidentResponse status(@PathVariable Long id,@RequestBody StatusRequest request) { return incidents.updateStatus(id,request.status()); }
    @GetMapping("/{id}/evidence/{fileId}") public ResponseEntity<byte[]> evidence(@PathVariable Long id,@PathVariable Long fileId) {
        var file=incidents.getEvidence(id,fileId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.getFilename(),StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options","nosniff").body(file.getContent());
    }
}
