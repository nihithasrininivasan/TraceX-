package com.traceroute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceroute.validation.ContactFormats;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ContactFormatsTest {
    @Test void sharedEmailAndPhoneEdgeCases() throws Exception {
        var cases = new ObjectMapper().readTree(getClass().getResourceAsStream("/contact-format-cases.json"));
        for (var value : cases.get("validEmails")) assertThat(ContactFormats.email(value.asText())).as(value.asText()).isTrue();
        for (var value : cases.get("invalidEmails")) assertThat(ContactFormats.email(value.asText())).as(value.asText()).isFalse();
        for (var value : cases.get("validSenders")) assertThat(ContactFormats.sender(value.asText())).as(value.asText()).isTrue();
        for (var value : cases.get("invalidSenders")) assertThat(ContactFormats.sender(value.asText())).as(value.asText()).isFalse();
        assertThat(ContactFormats.email("a".repeat(65)+"@example.com")).isFalse();
        assertThat(ContactFormats.email("a@"+"b".repeat(64)+".com")).isFalse();
        assertThat(ContactFormats.sender(null)).isTrue();
    }
    @Test void loginAndRegistrationEnforceTheEmailContract() throws Exception {
        var cases = new ObjectMapper().readTree(getClass().getResourceAsStream("/contact-format-cases.json"));
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String group : new String[]{"validEmails", "invalidEmails"}) {
                for (var value : cases.get(group)) {
                    boolean valid = group.equals("validEmails");
                    assertThat(validator.validate(new com.traceroute.dto.LoginRequest(value.asText(), "Password123!" )).isEmpty())
                        .as("login: " + value.asText()).isEqualTo(valid);
                    assertThat(validator.validate(new com.traceroute.dto.RegisterRequest("Student", value.asText(), "Password123!" )).isEmpty())
                        .as("register: " + value.asText()).isEqualTo(valid);
                }
            }
        }
    }
}
