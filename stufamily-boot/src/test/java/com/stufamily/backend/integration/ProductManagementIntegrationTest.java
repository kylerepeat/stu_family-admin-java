package com.stufamily.backend.integration;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductManagementIntegrationTest extends AbstractIntegrationTest {

    private static Long createdProductId;

    @Test
    @Order(1)
    void shouldListProductsWithDateRange() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(30);
        LocalDate endDate = today.plusDays(30);

        mockMvc.perform(withAuth(get("/api/admin/products")
                        .param("sale_start_at", startDate.toString())
                        .param("sale_end_at", endDate.toString())
                        .param("publish_status", "ON_SHELF")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").exists());

        rateLimitWait();
    }

    @Test
    @Order(2)
    void shouldCreateFamilyCardProduct() throws Exception {
        ProductRequest request = new ProductRequest(
                "FAMILY_CARD",
                "Integration Test Family Card",
                "Test Subtitle",
                "Test detail content for family card",
                List.of("https://example.com/test.png"),
                "Test Contact",
                "13800138000",
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMonths(6).toString(),
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMonths(3).toString(),
                "DRAFT",
                true,
                100,
                List.of(new FamilyCardPlanRequest(null, "MONTH", 1, 9900L, 5, true)),
                null
        );

        String response = mockMvc.perform(withAuth(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productType").value("FAMILY_CARD"))
                .andExpect(jsonPath("$.data.title").value("Integration Test Family Card"))
                .andExpect(jsonPath("$.data.publishStatus").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        createdProductId = objectMapper.readTree(response).path("data").path("id").asLong();

        rateLimitWait();
    }

    @Test
    @Order(3)
    void shouldGetProductDetail() throws Exception {
        if (createdProductId == null) {
            return;
        }

        mockMvc.perform(withAuth(get("/api/admin/products/" + createdProductId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(createdProductId))
                .andExpect(jsonPath("$.data.familyCardPlans").isArray());

        rateLimitWait();
    }

    @Test
    @Order(4)
    void shouldUpdateProduct() throws Exception {
        if (createdProductId == null) {
            return;
        }

        ProductRequest request = new ProductRequest(
                "FAMILY_CARD",
                "Updated Family Card Title",
                "Updated Subtitle",
                "Updated detail content",
                List.of("https://example.com/updated.png"),
                "Updated Contact",
                "13800138001",
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMonths(6).toString(),
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMonths(3).toString(),
                "DRAFT",
                false,
                50,
                List.of(new FamilyCardPlanRequest(null, "MONTH", 1, 19900L, 10, true)),
                null
        );

        mockMvc.perform(withAuth(put("/api/admin/products/" + createdProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Family Card Title"));

        rateLimitWait();
    }

    @Test
    @Order(5)
    void shouldPutProductOnShelf() throws Exception {
        if (createdProductId == null) {
            return;
        }

        mockMvc.perform(withAuth(post("/api/admin/products/" + createdProductId + "/on-shelf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publishStatus").value("ON_SHELF"));

        rateLimitWait();
    }

    @Test
    @Order(6)
    void shouldTakeProductOffShelf() throws Exception {
        if (createdProductId == null) {
            return;
        }

        mockMvc.perform(withAuth(post("/api/admin/products/" + createdProductId + "/off-shelf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publishStatus").value("OFF_SHELF"));

        rateLimitWait();
    }

    @Test
    @Order(7)
    void shouldCreateValueAddedServiceProduct() throws Exception {
        ProductRequest request = new ProductRequest(
                "VALUE_ADDED_SERVICE",
                "Integration Test VAS",
                "VAS Subtitle",
                "Test detail content for value added service",
                List.of(),
                "Service Contact",
                "13800138002",
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMonths(12).toString(),
                LocalDateTime.now().toString(),
                LocalDateTime.now().plusMonths(12).toString(),
                "ON_SHELF",
                false,
                50,
                null,
                List.of(new ValueAddedSkuRequest(null, "Basic Service x1", 5000L, true))
        );

        mockMvc.perform(withAuth(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productType").value("VALUE_ADDED_SERVICE"))
                .andExpect(jsonPath("$.data.valueAddedSkus").isArray())
                .andExpect(jsonPath("$.data.valueAddedSkus[0].title").value("Basic Service x1"));

        rateLimitWait();
    }

    @Test
    @Order(8)
    void shouldRejectProductWithoutRequiredPlan() throws Exception {
        ProductRequest request = new ProductRequest(
                "FAMILY_CARD",
                "Invalid Family Card",
                "Test",
                "Detail",
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                "DRAFT",
                false,
                0,
                null,
                null
        );

        mockMvc.perform(withAuth(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());

        rateLimitWait();
    }

    private record ProductRequest(
            String productType,
            String title,
            String subtitle,
            String detailContent,
            List<String> imageUrls,
            String contactName,
            String contactPhone,
            String serviceStartAt,
            String serviceEndAt,
            String saleStartAt,
            String saleEndAt,
            String publishStatus,
            Boolean top,
            Integer displayPriority,
            List<FamilyCardPlanRequest> familyCardPlans,
            List<ValueAddedSkuRequest> valueAddedSkus
    ) {
    }

    private record FamilyCardPlanRequest(
            Long id,
            String durationType,
            Integer durationMonths,
            Long priceCents,
            Integer maxFamilyMembers,
            Boolean enabled
    ) {
    }

    private record ValueAddedSkuRequest(
            Long id,
            String title,
            Long priceCents,
            Boolean enabled
    ) {
    }
}
