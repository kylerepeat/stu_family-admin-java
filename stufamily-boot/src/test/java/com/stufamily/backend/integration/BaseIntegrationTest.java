package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.boot.StuFamilyBackendApplication;
import com.stufamily.backend.shared.security.AuthAudience;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.shared.security.ReplayGuardFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(
    classes = StuFamilyBackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected ReplayGuardFilter replayGuardFilter;

    protected String adminToken;
    protected Long adminUserId = 1L;
    protected String adminUsername = "admin";

    @BeforeEach
    void setUpBase() throws Exception {
        ReplayGuardFilter.clearForTest();
        adminToken = jwtTokenProvider.createAccessToken(
            adminUserId, adminUsername, List.of("ADMIN"), AuthAudience.ADMIN, 0L);
        Thread.sleep(100);
    }

    @AfterEach
    void tearDownBase() throws Exception {
        ReplayGuardFilter.clearForTest();
    }

    protected String extractDataField(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        if (root.has("data")) {
            JsonNode data = root.get("data");
            if (data.isNull()) {
                return null;
            }
            return objectMapper.writeValueAsString(data);
        }
        return null;
    }

    protected Long extractIdFromData(MvcResult result) throws Exception {
        String data = extractDataField(result);
        if (data == null) {
            return null;
        }
        JsonNode dataNode = objectMapper.readTree(data);
        if (dataNode.has("id")) {
            return dataNode.get("id").asLong();
        }
        return null;
    }

    protected void waitForReplayGuard() throws InterruptedException {
        Thread.sleep(2100);
    }

    protected void waitForGetReplayGuard() throws InterruptedException {
        Thread.sleep(250);
    }
}
