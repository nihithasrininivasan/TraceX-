package com.traceroute;

import com.fasterxml.jackson.databind.*;
import com.traceroute.entity.*;
import com.traceroute.repository.UserRepository;
import com.traceroute.incident.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class IncidentIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder; @Autowired IncidentRepository incidents;
    @Autowired EvidenceRepository evidence; @Autowired AuditLogRepository audits;
    @Autowired IncidentRelationshipRepository relationships;
    static final String PASSWORD="IntegrationTest123!";
    @BeforeEach void prepare() {
        evidence.deleteAll(); relationships.deleteAll(); audits.deleteAll(); incidents.deleteAll(); users.deleteAll();
        users.save(new User("Student A","a@campus.test",encoder.encode(PASSWORD),Role.STUDENT));
        users.save(new User("Student B","b@campus.test",encoder.encode(PASSWORD),Role.STUDENT));
        users.save(new User("Test Admin","admin@campus.test",encoder.encode(PASSWORD),Role.ADMIN));
    }
    String body(String title) throws Exception {
        return json.writeValueAsString(java.util.Map.of("title",title,"incidentType","Phishing","description","Safe sample phishing report for integration testing.","suspiciousUrl","https://fakebank.test/login","senderEmail","+91 90000 00000","severity","High"));
    }
    long create(String email) throws Exception {
        var result=mvc.perform(post("/api/incidents").with(httpBasic(email,PASSWORD)).contentType(MediaType.APPLICATION_JSON).content(body("Sample phishing")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.user.email").doesNotExist()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("incidentId").asLong();
    }
    @Test void fullWorkflowPersistsRelationshipsGraphAdminUpdateAndAudit() throws Exception {
        long first=create("a@campus.test"), second=create("b@campus.test");
        assertThat(incidents.count()).isEqualTo(2); assertThat(relationships.count()).isEqualTo(1);
        assertThat(relationships.findAll().getFirst().getMatchScore()).isEqualTo(1.0);
        mvc.perform(get("/api/graph").with(httpBasic("admin@campus.test",PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nodes[?(@.id == 'domain:fakebank.test')]").isNotEmpty())
            .andExpect(jsonPath("$.edges[?(@.target == 'domain:fakebank.test')]").value(org.hamcrest.Matchers.hasSize(2)));
        mvc.perform(get("/api/incidents/"+first+"/related").with(httpBasic("admin@campus.test",PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].incidentId").value(second));
        mvc.perform(put("/api/incidents/"+first+"/status").with(httpBasic("admin@campus.test",PASSWORD)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Resolved\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(get("/api/incidents").with(httpBasic("a@campus.test",PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].status").value("RESOLVED"));
        assertThat(audits.count()).isEqualTo(3);
    }
    @Test void studentsCannotReadOthersReportsOrChangeStatus() throws Exception {
        long id=create("a@campus.test");
        mvc.perform(get("/api/incidents/"+id).with(httpBasic("b@campus.test",PASSWORD))).andExpect(status().isNotFound());
        mvc.perform(put("/api/incidents/"+id+"/status").with(httpBasic("a@campus.test",PASSWORD)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/network/graph").with(httpBasic("b@campus.test",PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.nodes").isEmpty());
        mvc.perform(get("/api/relationships").with(httpBasic("b@campus.test",PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }
    @Test void multipartEvidenceIsStoredAndAccessControlled() throws Exception {
        byte[] bytes="%PDF-1.4\nTest evidence\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        var details=new MockMultipartFile("incident","incident.json","application/json",body("PDF evidence").getBytes());
        var file=new MockMultipartFile("evidence","safe-proof.pdf","application/pdf",bytes);
        var result=mvc.perform(multipart("/api/incidents").file(details).file(file).with(httpBasic("a@campus.test",PASSWORD)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.evidenceFiles[0].filename").value("safe-proof.pdf")).andReturn();
        var response=json.readTree(result.getResponse().getContentAsString());
        String path="/api/incidents/"+response.get("incidentId").asLong()+"/evidence/"+response.get("evidenceFiles").get(0).get("id").asLong();
        mvc.perform(get(path).with(httpBasic("a@campus.test",PASSWORD))).andExpect(status().isOk()).andExpect(content().bytes(bytes));
        mvc.perform(get(path).with(httpBasic("b@campus.test",PASSWORD))).andExpect(status().isNotFound());
        assertThat(evidence.count()).isEqualTo(1);
    }
    @Test void invalidEvidenceRollsBackIncidentAndAudit() throws Exception {
        mvc.perform(multipart("/api/incidents").file(new MockMultipartFile("incident","incident.json","application/json",body("Bad evidence").getBytes()))
            .file(new MockMultipartFile("evidence","bad.pdf","application/pdf","not-a-pdf".getBytes())).with(httpBasic("a@campus.test",PASSWORD)))
            .andExpect(status().isBadRequest());
        assertThat(incidents.count()).isZero(); assertThat(audits.count()).isZero();
    }
    @Test void badUrlAndStatusReturnUsefulErrors() throws Exception {
        mvc.perform(post("/api/incidents").with(httpBasic("a@campus.test",PASSWORD)).contentType(MediaType.APPLICATION_JSON).content(body("Bad url").replace("https://fakebank.test/login","file:///etc/passwd")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        long id=create("a@campus.test");
        mvc.perform(put("/api/incidents/"+id+"/status").with(httpBasic("admin@campus.test",PASSWORD)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"unknown\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void graphEdgesAlwaysReferenceVisibleNodes() throws Exception {
        create("a@campus.test"); create("b@campus.test");
        for(String email:java.util.List.of("a@campus.test","admin@campus.test")) {
            var result=mvc.perform(get("/api/graph").with(httpBasic(email,PASSWORD))).andExpect(status().isOk()).andReturn();
            var graph=json.readTree(result.getResponse().getContentAsString());
            var ids=new java.util.HashSet<String>(); graph.get("nodes").forEach(node -> assertThat(ids.add(node.get("id").asText())).isTrue());
            graph.get("edges").forEach(edge -> {assertThat(ids).contains(edge.get("source").asText());assertThat(ids).contains(edge.get("target").asText());});
        }
    }
    @Test void healthAndNetworkEndpointsRequireAppropriateAccess() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.database").value("UP"));
        mvc.perform(get("/api/network/graph")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/network/dns?domain=fakebank.test").with(httpBasic("a@campus.test",PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.ipAddress").value("192.0.2.10"));
        mvc.perform(get("/api/network/tcp?message=TraceX-test").with(httpBasic("a@campus.test",PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.receivedMessage").value("TraceX-test"));
    }
}
