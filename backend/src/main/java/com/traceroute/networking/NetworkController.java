
package com.traceroute.networking;

import com.traceroute.incident.dto.IncidentResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/network")
public class NetworkController {

    private final DNSLookupService dnsLookupService;
    private final IPAnalysisService ipAnalysisService;
    private final TCPDemoService tcpDemoService;

    // Constructor
    public NetworkController(
            DNSLookupService dnsLookupService,
            IPAnalysisService ipAnalysisService,
            TCPDemoService tcpDemoService) {

        this.dnsLookupService = dnsLookupService;
        this.ipAnalysisService = ipAnalysisService;
        this.tcpDemoService = tcpDemoService;
    }

    // 1. DNS LOOKUP
    @GetMapping("/dns")
    public Map<String, String> performDNSLookup(
            @RequestParam String domain) {

        String ipAddress =
                dnsLookupService.lookupIP(domain);

        return Map.of(
                "domain", domain,
                "ipAddress", ipAddress,
                "lookupType", "SAMPLE_DNS"
        );
    }

    // 2. IP ANALYSIS
    @GetMapping("/ip")
    public List<IncidentResponse> analyzeIP(
            @RequestParam String address) {

        return ipAnalysisService.findRelatedIncidents(address);
    }

    // 3. COUNT RELATED INCIDENTS
    @GetMapping("/ip/count")
    public Map<String, Object> countIPMatches(
            @RequestParam String address) {

        int count =
                ipAnalysisService.countRelatedIncidents(address);

        return Map.of(
                "ipAddress", address,
                "relatedIncidentCount", count
        );
    }

    // 4. TCP CLIENT-SERVER COMMUNICATION
    @GetMapping("/tcp")
    public Map<String, Object> demonstrateTCP(
            @RequestParam(defaultValue = "Hello TraceRoute")
            String message) {

        return tcpDemoService.demonstrateTCP(message);
    }
}
