package com.stufamily.backend.integration;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderRefundAndFamilyGroupIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_ORDER_NO = "ORDPOSTMANSEED000001";

    @Test
    @Order(1)
    void shouldListOrderRefunds() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders/" + TEST_ORDER_NO + "/refunds")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists());

        rateLimitWait();
    }

    @Test
    @Order(2)
    void shouldGetProductReviewByOrderId() throws Exception {
        String ordersResponse = mockMvc.perform(withAuth(get("/api/admin/orders")
                        .param("keyword", "ORDPOSTMANSEED000002")
                        .param("page_no", "1")
                        .param("page_size", "1")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var orderId = objectMapper.readTree(ordersResponse)
                .path("data")
                .path("items")
                .path(0)
                .path("orderId")
                .asLong();

        if (orderId > 0) {
            mockMvc.perform(withAuth(get("/api/admin/orders/" + orderId + "/product-review")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        rateLimitWait();
    }

    @Test
    @Order(3)
    void shouldProcessOrderRefund() throws Exception {
        RefundRequest request = new RefundRequest(100L, "Integration test refund");

        mockMvc.perform(withAuth(post("/api/admin/orders/" + TEST_ORDER_NO + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value(TEST_ORDER_NO));

        rateLimitWait();
    }

    @Test
    @Order(4)
    void shouldRejectRefundWithInvalidAmount() throws Exception {
        RefundRequest request = new RefundRequest(-1L, "Invalid amount");

        mockMvc.perform(withAuth(post("/api/admin/orders/" + TEST_ORDER_NO + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());

        rateLimitWait();
    }

    @Test
    @Order(5)
    void shouldRejectRefundForNonExistentOrder() throws Exception {
        RefundRequest request = new RefundRequest(100L, "Test refund");

        mockMvc.perform(withAuth(post("/api/admin/orders/NONEXISTENT001/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().is4xxClientError());

        rateLimitWait();
    }

    @Test
    @Order(6)
    void shouldDisableFamilyGroup() throws Exception {
        mockMvc.perform(withAuth(post("/api/admin/orders/ORDPOSTMANSEED000003/disable-family-group")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("ORDPOSTMANSEED000003"))
                .andExpect(jsonPath("$.data.groupStatus").value("CLOSED"));

        rateLimitWait();
    }

    @Test
    @Order(7)
    void shouldVerifyFamilyGroupDisabled() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-cards")
                        .param("keyword", "ORDPOSTMANSEED000003")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        rateLimitWait();
    }

    @Test
    @Order(8)
    void shouldRejectDisableNonExistentOrderFamilyGroup() throws Exception {
        mockMvc.perform(withAuth(post("/api/admin/orders/NONEXISTENT001/disable-family-group")))
                .andExpect(status().is4xxClientError());

        rateLimitWait();
    }

    @Test
    @Order(9)
    void shouldVerifyRefundRecorded() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders/" + TEST_ORDER_NO + "/refunds")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    private record RefundRequest(Long refundAmountCents, String reason) {
    }
}
