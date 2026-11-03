
package com.traceroute.networking;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class DNSLookupService {

    // Sample DNS records for demonstration
    private final Map<String, String> dnsRecords = Map.of(
        "fakebank.test", "192.0.2.10",
        "fakegift.test", "192.0.2.10",
        "fakecollege.test", "192.0.2.30"
    );

    // Find IP address using domain name
    public String lookupIP(String domain) {

        if (domain == null || domain.isBlank()) {
            return "Invalid domain";
        }

        return dnsRecords.getOrDefault(
            domain.toLowerCase(),
            "No sample DNS record found"
        );
    }
}
