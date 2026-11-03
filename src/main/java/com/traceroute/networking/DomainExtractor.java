
package com.traceroute.networking;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.IDN;
import java.util.Locale;

@Component
public class DomainExtractor {

    public String extractDomain(String rawUrl) {

        // Check whether URL is empty
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        try {
            // Parse the submitted URL
            URI uri = new URI(rawUrl.trim());

            // Allow only HTTP and HTTPS
            String scheme = uri.getScheme();

            if (scheme == null ||
                !(scheme.equalsIgnoreCase("http") ||
                  scheme.equalsIgnoreCase("https"))) {

                throw new IllegalArgumentException(
                    "Only HTTP and HTTPS URLs are allowed"
                );
            }

            // Extract hostname
            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException(
                    "Invalid URL: hostname not found"
                );
            }

            // Convert hostname to standard format
            return IDN.toASCII(host).toLowerCase(Locale.ROOT);

        } catch (URISyntaxException e) {

            throw new IllegalArgumentException(
                "Invalid URL format"
            );
        }
    }
}
