package com.traceroute.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "[\\x20-\\x7E]+", message = "must use printable ASCII characters (BCrypt limit: 72 bytes)") String password) {
    @Override public String toString() { return "RegisterRequest[redacted]"; }
}
