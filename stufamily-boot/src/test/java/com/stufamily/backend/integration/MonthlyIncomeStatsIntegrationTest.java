package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonthlyIncomeStatsIntegrationTest extends AbstractIntegrationTest {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Test
    void shouldGetMonthlyIncomeStatsWithoutFilters() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray())
                .andExpect(jsonPath("$.data.monthlyRefundIncome").isArray())
                .andExpect(jsonPath("$.data.totalIncomeCents").exists())
                .andExpect(jsonPath("$.data.totalRefundCents").exists())
                .andExpect(jsonPath("$.data.netIncomeCents").exists());

        rateLimitWait();
    }

    @Test
    void shouldGetMonthlyIncomeStatsWithDateRange() throws Exception {
        LocalDate now = LocalDate.now();
        String startMonth = now.minusMonths(3).format(MONTH_FORMATTER);
        String endMonth = now.plusMonths(1).format(MONTH_FORMATTER);

        mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")
                        .param("start_month", startMonth)
                        .param("end_month", endMonth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray())
                .andExpect(jsonPath("$.data.totalIncomeCents").exists());

        rateLimitWait();
    }

    @Test
    void shouldGetMonthlyIncomeStatsFilteredByFamilyCardType() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")
                        .param("product_type", "FAMILY_CARD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray())
                .andExpect(jsonPath("$.data.totalIncomeCents").exists());

        rateLimitWait();
    }

    @Test
    void shouldGetMonthlyIncomeStatsFilteredByValueAddedService() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")
                        .param("product_type", "VALUE_ADDED_SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray())
                .andExpect(jsonPath("$.data.totalIncomeCents").exists());

        rateLimitWait();
    }

    @Test
    void shouldGetMonthlyIncomeStatsFilteredByProductId() throws Exception {
        String productsResponse = mockMvc.perform(withAuth(get("/api/admin/products")
                        .param("sale_start_at", LocalDate.now().minusDays(30).toString())
                        .param("sale_end_at", LocalDate.now().plusDays(30).toString())
                        .param("page_no", "1")
                        .param("page_size", "1")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var productId = objectMapper.readTree(productsResponse)
                .path("data")
                .path("items")
                .path(0)
                .path("id")
                .asLong();

        if (productId > 0) {
            mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")
                            .param("product_id", String.valueOf(productId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        rateLimitWait();
    }

    @Test
    void shouldGetMonthlyIncomeStatsWithAllFiltersCombined() throws Exception {
        LocalDate now = LocalDate.now();
        String startMonth = now.minusMonths(6).format(MONTH_FORMATTER);
        String endMonth = now.format(MONTH_FORMATTER);

        mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")
                        .param("start_month", startMonth)
                        .param("end_month", endMonth)
                        .param("product_type", "FAMILY_CARD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray());

        rateLimitWait();
    }

    @Test
    void shouldVerifyNetIncomeCalculation() throws Exception {
        String response = mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var data = objectMapper.readTree(response).path("data");
        long totalIncome = data.path("totalIncomeCents").asLong();
        long totalRefund = data.path("totalRefundCents").asLong();
        long netIncome = data.path("netIncomeCents").asLong();

        assert netIncome == totalIncome - totalRefund;

        rateLimitWait();
    }

    @Test
    void shouldVerifyMonthlyAmountViewStructure() throws Exception {
        String response = mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var monthlyIncome = objectMapper.readTree(response)
                .path("data")
                .path("monthlyTotalIncome");

        if (monthlyIncome.isArray() && monthlyIncome.size() > 0) {
            var firstMonth = monthlyIncome.get(0);
            assert firstMonth.has("month");
            assert firstMonth.has("amountCents");
        }

        rateLimitWait();
    }

    @Test
    void shouldHandleInvalidProductTypeGracefully() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/orders/monthly-income-stats")
                        .param("product_type", "INVALID_TYPE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        rateLimitWait();
    }

    @Test
    void shouldRejectUnauthorizedStatsQuery() throws Exception {
        mockMvc.perform(get("/api/admin/orders/monthly-income-stats"))
                .andExpect(status().isUnauthorized());
    }
}
