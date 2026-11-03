package com.traceroute.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) @Pattern(regexp = "[\\x20-\\x7E]+", message = "must use printable ASCII characters") String password) {
    @Override public String toString() { return "LoginRequest[redacted]"; }
}
