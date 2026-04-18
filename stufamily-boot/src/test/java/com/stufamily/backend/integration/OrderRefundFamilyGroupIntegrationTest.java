package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("订单退款与家庭组停用集成测试")
public class OrderRefundFamilyGroupIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/admin/orders";

    @Nested
    @DisplayName("订单退款")
    class OrderRefundTests {

        @Test
        @DisplayName("查询订单退款记录列表-成功")
        void listOrderRefunds_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ORDPOSTMANSEED000001/refunds")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询订单商品评价-成功")
        void getProductReviewByOrderId_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/2/product-review")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("查询订单商品评价-订单不存在")
        void getProductReviewByOrderId_orderNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999999/product-review")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("家庭组停用")
    class FamilyGroupDisableTests {

        @Test
        @DisplayName("查询订单退款记录-订单不存在")
        void listOrderRefunds_orderNotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ORDNOTEXIST123456/refunds")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("订单退款流程")
    class OrderRefundFlowTests {

        @Test
        @DisplayName("订单退款-订单不存在返回错误")
        void refundOrder_orderNotFound() throws Exception {
            waitForReplayGuard();

            String refundRequest = "{\"refundAmountCents\":1000,\"reason\":\"test refund\"}";

            mockMvc.perform(post(BASE_URL + "/ORDNOTEXIST123456/refund")
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refundRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("家庭组停用流程")
    class FamilyGroupDisableFlowTests {

        @Test
        @DisplayName("停用家庭组-订单不存在返回错误")
        void disableFamilyGroup_orderNotFound() throws Exception {
            waitForReplayGuard();

            mockMvc.perform(post(BASE_URL + "/ORDNOTEXIST123456/disable-family-group")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        }
    }
}
