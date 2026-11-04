package com.traceroute.incident.service;

import com.traceroute.incident.dto.*;
import com.traceroute.incident.entity.*;
import com.traceroute.incident.repository.*;
import com.traceroute.entity.*;
import com.traceroute.repository.UserRepository;
import com.traceroute.networking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.io.IOException;

@Service @Transactional
public class IncidentService {
    private final IncidentRepository incidents;
    private final EvidenceRepository evidence;
    private final AuditLogRepository audits;
    private final UserRepository users;
    private final DomainExtractor domains;
    private final DNSLookupService dns;
    private final RelationshipService relationships;
    private final IncidentRelationshipRepository links;
    public IncidentService(IncidentRepository incidents, EvidenceRepository evidence, AuditLogRepository audits,
            UserRepository users, DomainExtractor domains, DNSLookupService dns, RelationshipService relationships,
            IncidentRelationshipRepository links) {
        this.incidents=incidents; this.evidence=evidence; this.audits=audits; this.users=users;
        this.domains=domains; this.dns=dns; this.relationships=relationships; this.links=links;
    }
    public User currentUser() {
        return users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
    private boolean visible(Incident incident, User user) {
        return user.getRole()==Role.ADMIN || incident.getUser()!=null && incident.getUser().getId().equals(user.getId());
    }
    public Incident authorized(Long id) {
        var incident = incidents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found"));
        if (!visible(incident,currentUser())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found");
        return incident;
    }
    private IncidentResponse response(Incident incident) {
        var files=evidence.findByIncidentIncidentId(incident.getIncidentId()).stream()
            .map(file -> new IncidentResponse.EvidenceSummary(file.getEvidenceId(),file.getFilename(),file.getContentType())).toList();
        return IncidentResponse.from(incident,files);
    }
    @Transactional(readOnly=true)
    public List<IncidentResponse> getAllIncidents() {
        var user=currentUser();
        return incidents.findAll().stream().filter(incident -> visible(incident,user))
            .sorted(Comparator.comparing(Incident::getIncidentId).reversed()).map(this::response).toList();
    }
    @Transactional(readOnly=true)
    public IncidentResponse getIncidentById(Long id) { return response(authorized(id)); }
    public IncidentResponse createIncident(IncidentRequest request, MultipartFile file) {
        var actor=currentUser();
        if (request.getSeverity()!=null && !List.of("Low","Medium","High").contains(request.getSeverity())) throw new IllegalArgumentException("Invalid severity");
        var incident=new Incident();
        incident.setUser(actor); incident.setIncidentType(request.getIncidentType().trim());
        incident.setTitle(request.getTitle()==null || request.getTitle().isBlank() ? request.getIncidentType()+" report" : request.getTitle().trim());
        incident.setDescription(request.getDescription().trim()); incident.setSuspiciousUrl(request.getSuspiciousUrl());
        incident.setSenderEmail(request.getSenderEmail()); incident.setSeverity(request.getSeverity()==null ? "Medium" : request.getSeverity());
        incident.setCustomType(request.getCustomType());
        String domain=domains.extractDomain(request.getSuspiciousUrl()); incident.setDomain(domain);
        String ip=request.getSampleIp();
        if(ip!=null && !ip.isBlank()) {
            if(!ip.matches("(\\d{1,3}\\.){3}\\d{1,3}") || Arrays.stream(ip.split("\\.")).anyMatch(part -> Integer.parseInt(part)>255)) throw new IllegalArgumentException("Invalid IPv4 address");
        } else { String sample=dns.lookupIP(domain); ip=sample.matches("(\\d{1,3}\\.){3}\\d{1,3}") ? sample : null; }
        incident.setSampleIp(ip); incident.setStatus("PENDING"); incidents.save(incident);
        if(file!=null && !file.isEmpty()) storeEvidence(incident,file);
        relationships.detectRelationships(incident);
        audits.save(new AuditLog(actor,"CREATE_INCIDENT:"+incident.getIncidentId()));
        return response(incident);
    }
    private void storeEvidence(Incident incident, MultipartFile file) {
        if(file.getSize()>5*1024*1024) throw new IllegalArgumentException("Evidence must be at most 5 MB");
        if(!List.of("image/png","image/jpeg","application/pdf").contains(file.getContentType()==null?"":file.getContentType())) throw new IllegalArgumentException("Evidence must be PNG, JPG or PDF");
        try {
            byte[] bytes=file.getBytes();
            boolean signature = switch(file.getContentType()) {
                case "image/png" -> bytes.length>8 && bytes[0]==(byte)0x89 && bytes[1]=='P' && bytes[2]=='N' && bytes[3]=='G';
                case "image/jpeg" -> bytes.length>3 && bytes[0]==(byte)0xff && bytes[1]==(byte)0xd8 && bytes[2]==(byte)0xff;
                default -> bytes.length>5 && bytes[0]=='%' && bytes[1]=='P' && bytes[2]=='D' && bytes[3]=='F' && bytes[4]=='-';
            };
            if(!signature) throw new IllegalArgumentException("Evidence content does not match its file type");
            String name=Optional.ofNullable(file.getOriginalFilename()).orElse("evidence").replace('\\','/');
            name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]", "");
            if(name.length()>255) name=name.substring(name.length()-255);
            evidence.save(new Evidence(incident,name,file.getContentType(),bytes));
        } catch(IOException exception) { throw new IllegalArgumentException("Unable to read evidence"); }
    }
    @Transactional(readOnly=true)
    public Evidence getEvidence(Long id, Long fileId) {
        authorized(id);
        return evidence.findById(fileId).filter(file -> file.getIncident().getIncidentId().equals(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Evidence not found"));
    }
    public IncidentResponse updateStatus(Long id, String status) {
        var actor=currentUser();
        if(actor.getRole()!=Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator access required");
        String normalized=status==null ? "" : status.toUpperCase(Locale.ROOT);
        if(!List.of("PENDING","INVESTIGATING","RESOLVED").contains(normalized)) throw new IllegalArgumentException("Invalid incident status");
        var incident=authorized(id); incident.setStatus(normalized); incidents.save(incident);
        audits.save(new AuditLog(actor,"STATUS:"+id+":"+normalized)); return response(incident);
    }
    @Transactional(readOnly=true)
    public List<IncidentResponse> related(Long id) {
        authorized(id); var actor=currentUser();
        Set<Long> ids=new HashSet<>();
        links.findBySourceIncidentIdOrTargetIncidentId(id,id).forEach(link -> ids.add(link.getSourceIncidentId().equals(id)?link.getTargetIncidentId():link.getSourceIncidentId()));
        return incidents.findAllById(ids).stream().filter(incident -> visible(incident,actor)).map(this::response).toList();
    }
    @Transactional(readOnly=true)
    public List<IncidentRelationship> visibleRelationships() {
        Set<Long> ids=new HashSet<>(getAllIncidents().stream().map(IncidentResponse::incidentId).toList());
        return links.findAll().stream().filter(link -> ids.contains(link.getSourceIncidentId())&&ids.contains(link.getTargetIncidentId())).toList();
    }
}
