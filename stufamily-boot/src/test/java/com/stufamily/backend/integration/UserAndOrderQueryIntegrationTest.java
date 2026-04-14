package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAndOrderQueryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldListFilterOptions() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/filter-options")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productPublishStatuses").isArray())
                .andExpect(jsonPath("$.data.weixinUserStatuses").isArray())
                .andExpect(jsonPath("$.data.orderStatuses").isArray())
                .andExpect(jsonPath("$.data.orderTypes").isArray())
                .andExpect(jsonPath("$.data.familyCardStatuses").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListWeixinUsersWithPagination() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/weixin-users")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        rateLimitWait();
    }

    @Test
    void shouldListWeixinUsersWithKeyword() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/weixin-users")
                        .param("keyword", "postman")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListWeixinUsersWithStatusFilter() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/weixin-users")
                        .param("status", "ACTIVE")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListOrdersWithPagination() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists());

        rateLimitWait();
    }

    @Test
    void shouldListOrdersWithStatusFilter() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders")
                        .param("order_status", "PAID")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListOrdersWithTypeFilter() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders")
                        .param("order_type", "FAMILY_CARD")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListOrdersWithKeyword() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders")
                        .param("keyword", "POSTMAN")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyCardsWithPagination() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-cards")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyCardsWithStatusFilter() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-cards")
                        .param("status", "ACTIVE")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyCardsWithKeyword() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-cards")
                        .param("keyword", "POSTMAN")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyCheckInsWithPagination() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-checkins")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists());

        rateLimitWait();
    }

    @Test
    void shouldRejectUnauthorizedRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/weixin-users")
                        .param("page_no", "1")
                        .param("page_size", "10"))
                .andExpect(status().isUnauthorized());
    }
}
