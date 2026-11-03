package com.traceroute;

import com.traceroute.entity.*;
import com.traceroute.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {
    private static final String BODY = """
            {"name":"Student A","email":"student@campus.edu","password":"StrongPass123!"}
            """;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @BeforeEach void clean() { users.deleteAll(); }
    void register() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated());
    }
    @Test void registrationHashesPasswordAndReturnsOnlyPublicFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Student A")).andExpect(jsonPath("$.email").value("student@campus.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT")).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        var user = users.findByEmail("student@campus.edu").orElseThrow();
        assertThat(user.getPasswordHash()).startsWith("$2").isNotEqualTo("StrongPass123!");
        assertThat(encoder.matches("StrongPass123!", user.getPasswordHash())).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
    }
    @Test void duplicateEmailIsCaseInsensitive() throws Exception {
        register();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("student@campus.edu", "STUDENT@campus.edu")))
                .andExpect(status().isConflict());
        assertThat(users.count()).isEqualTo(1);
    }
    @Test void invalidInputReturns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.name").exists()).andExpect(jsonPath("$.errors.password").exists());
        assertThat(users.count()).isZero();
    }
    @Test void roleCannotBeEscalatedAtRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("}", ",\"role\":\"ADMIN\"}")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT"));
    }
    @Test void correctLoginIsStateless() throws Exception {
        register();
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }
    @Test void incorrectAndUnknownLoginReturn401() throws Exception {
        register();
        for (String body : new String[]{BODY.replace("StrongPass123!", "incorrect"), BODY.replace("student@campus.edu", "unknown@campus.edu")}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid credentials"));
        }
    }
    @Test void protectedRoutesRequireAuthentication() throws Exception {
        for (String path : new String[]{"/api/users/me", "/api/users", "/api/incidents", "/api/incidents/1", "/api/graph"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
    @Test void basicAuthenticationLoadsDatabaseUser() throws Exception {
        register();
        mvc.perform(get("/api/users/me").with(httpBasic("STUDENT@campus.edu", "StrongPass123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("student@campus.edu"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/users/me").with(httpBasic("student@campus.edu", "wrong"))).andExpect(status().isUnauthorized());
    }
    @Test void studentCannotListUsers() throws Exception {
        register();
        mvc.perform(get("/api/users").with(httpBasic("student@campus.edu", "StrongPass123!"))).andExpect(status().isForbidden());
    }
    @Test void adminCanListUsers() throws Exception {
        register();
        users.saveAndFlush(new User("Admin", "admin@campus.edu", encoder.encode("AdminPass123!"), Role.ADMIN));
        mvc.perform(get("/api/users").with(httpBasic("admin@campus.edu", "AdminPass123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
    @Test void corsAllowsOnlyFrontendOrigin() throws Exception {
        mvc.perform(options("/api/users/me").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/users/me").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }
    @Test void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
    @Test void bcryptOversizedPasswordRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY.replace("StrongPass123!", "a".repeat(73))))
                .andExpect(status().isBadRequest());
    }
}
