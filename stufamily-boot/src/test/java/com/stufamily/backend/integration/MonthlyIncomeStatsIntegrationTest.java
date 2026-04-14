package com.stufamily.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("月度收入统计集成测试")
public class MonthlyIncomeStatsIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/admin/orders";

    @Nested
    @DisplayName("月度收入统计查询")
    class MonthlyIncomeStatsTests {

        @Test
        @DisplayName("查询月度收入统计-成功返回统计数据")
        void monthlyIncomeStats_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("查询月度收入统计-按时间范围筛选")
        void monthlyIncomeStats_filterByTimeRange() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("start_month", "2024-01")
                    .param("end_month", "2024-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("查询月度收入统计-按商品类型筛选")
        void monthlyIncomeStats_filterByProductType() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("product_type", "FAMILY_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("查询月度收入统计-按商品ID筛选")
        void monthlyIncomeStats_filterByProductId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("product_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("查询月度收入统计-组合筛选条件")
        void monthlyIncomeStats_combinedFilters() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("start_month", "2024-01")
                    .param("end_month", "2024-12")
                    .param("product_type", "FAMILY_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("查询月度收入统计-未授权返回401")
        void monthlyIncomeStats_unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .param("start_month", "2024-01")
                    .param("end_month", "2024-12"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("收入统计与订单关联")
    class IncomeStatsOrderRelationTests {

        @Test
        @DisplayName("查询月度收入统计-验证数据结构")
        void monthlyIncomeStats_verifyDataStructure() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("查询月度收入统计-增值服务类型")
        void monthlyIncomeStats_valueAddedService() throws Exception {
            mockMvc.perform(get(BASE_URL + "/monthly-income-stats")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("product_type", "VALUE_ADDED_SERVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
        }
    }
}
