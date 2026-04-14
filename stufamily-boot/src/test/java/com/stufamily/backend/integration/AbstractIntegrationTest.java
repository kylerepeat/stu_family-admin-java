package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.boot.StuFamilyBackendApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = StuFamilyBackendApplication.class)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected void rateLimitWait() throws InterruptedException {
        Thread.sleep(3100);
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder) throws Exception {
        String token = TestAuthManager.getAdminToken(mockMvc, objectMapper);
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}

