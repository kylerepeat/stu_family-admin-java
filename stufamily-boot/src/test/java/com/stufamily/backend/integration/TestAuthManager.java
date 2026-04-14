package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public final class TestAuthManager {

    private static final ReentrantLock lock = new ReentrantLock();
    private static volatile String adminToken;
    private static volatile boolean initialized = false;

    private static final String LOGIN_USERNAME = "admin";
    private static final String LOGIN_PASSWORD = "ChangeMe@2026!";

    private TestAuthManager() {
    }

    public static String getAdminToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        if (!initialized) {
            lock.lock();
            try {
                if (!initialized) {
                    authenticate(mockMvc, objectMapper);
                    initialized = true;
                }
            } finally {
                lock.unlock();
            }
        }
        return adminToken;
    }

    private static void authenticate(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        Thread.sleep(5000);

        String loginContent = objectMapper.writeValueAsString(new LoginRequest(LOGIN_USERNAME, LOGIN_PASSWORD));

        String response = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginContent))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 200) {
                        throw new AssertionError("Login failed with status: " + status + ", response: " + result.getResponse().getContentAsString());
                    }
                })
                .andReturn()
                .getResponse()
                .getContentAsString();

        adminToken = objectMapper.readTree(response).path("data").path("accessToken").asText();

        if (adminToken == null || adminToken.isEmpty()) {
            throw new AssertionError("Failed to get admin token, response: " + response);
        }

        Thread.sleep(1000);
    }

    public static void reset() {
        lock.lock();
        try {
            initialized = false;
            adminToken = null;
        } finally {
            lock.unlock();
        }
    }

    private record LoginRequest(String username, String password) {
    }
}
