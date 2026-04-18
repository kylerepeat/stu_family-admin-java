package com.stufamily.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("用户与订单查询集成测试")
public class UserOrderQueryIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/admin";

    @Nested
    @DisplayName("筛选下拉选项查询")
    class FilterOptionsTests {

        @Test
        @DisplayName("查询筛选下拉选项-成功返回所有选项")
        void listFilterOptions_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/filter-options")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productPublishStatuses").isArray())
                .andExpect(jsonPath("$.data.weixinUserStatuses").isArray())
                .andExpect(jsonPath("$.data.orderStatuses").isArray())
                .andExpect(jsonPath("$.data.orderTypes").isArray())
                .andExpect(jsonPath("$.data.familyCardStatuses").isArray());
        }
    }

    @Nested
    @DisplayName("微信用户查询")
    class WechatUserQueryTests {

        @Test
        @DisplayName("查询微信用户列表-成功返回分页数据")
        void listWechatUsers_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/weixin-users")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("查询微信用户列表-按状态筛选")
        void listWechatUsers_filterByStatus() throws Exception {
            mockMvc.perform(get(BASE_URL + "/weixin-users")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("status", "ACTIVE")
                    .param("page_no", "1")
                    .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询微信用户列表-按关键词搜索")
        void listWechatUsers_filterByKeyword() throws Exception {
            mockMvc.perform(get(BASE_URL + "/weixin-users")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("keyword", "postman")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询微信用户列表-未授权返回401")
        void listWechatUsers_unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/weixin-users")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("订单查询")
    class OrderQueryTests {

        @Test
        @DisplayName("查询订单列表-成功返回分页数据")
        void listOrders_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/orders")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("查询订单列表-按订单状态筛选")
        void listOrders_filterByStatus() throws Exception {
            mockMvc.perform(get(BASE_URL + "/orders")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("order_status", "PAID")
                    .param("page_no", "1")
                    .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询订单列表-按订单类型筛选")
        void listOrders_filterByType() throws Exception {
            mockMvc.perform(get(BASE_URL + "/orders")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("order_type", "FAMILY_CARD")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询订单列表-按关键词搜索")
        void listOrders_filterByKeyword() throws Exception {
            mockMvc.perform(get(BASE_URL + "/orders")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("keyword", "ORDPOSTMAN")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询订单列表-返回买家微信信息")
        void listOrders_containsWechatUserInfo() throws Exception {
            mockMvc.perform(get(BASE_URL + "/orders")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("order_status", "PAID")
                    .param("page_no", "1")
                    .param("page_size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].orderNo").exists())
                .andExpect(jsonPath("$.data.items[0].buyerOpenid").exists())
                .andExpect(jsonPath("$.data.items[0].buyerNickname").exists());
        }
    }

    @Nested
    @DisplayName("家庭卡查询")
    class FamilyCardQueryTests {

        @Test
        @DisplayName("查询家庭卡列表-成功返回分页数据")
        void listFamilyCards_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-cards")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("查询家庭卡列表-按状态筛选")
        void listFamilyCards_filterByStatus() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-cards")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("status", "ACTIVE")
                    .param("page_no", "1")
                    .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询家庭卡列表-按关键词搜索")
        void listFamilyCards_filterByKeyword() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-cards")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("keyword", "G")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询家庭卡列表-返回归属用户微信信息")
        void listFamilyCards_containsOwnerWechatInfo() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-cards")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("status", "ACTIVE")
                    .param("page_no", "1")
                    .param("page_size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].groupNo").exists())
                .andExpect(jsonPath("$.data.items[0].ownerOpenid").exists())
                .andExpect(jsonPath("$.data.items[0].ownerNickname").exists());
        }
    }

    @Nested
    @DisplayName("打卡记录查询")
    class FamilyCheckInQueryTests {

        @Test
        @DisplayName("查询打卡记录列表-成功返回分页数据")
        void listFamilyCheckIns_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-checkins")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));
        }

        @Test
        @DisplayName("查询打卡记录列表-按家庭成员ID筛选")
        void listFamilyCheckIns_filterByFamilyMemberId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-checkins")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("family_member_id", "1")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询打卡记录列表-按微信用户ID筛选")
        void listFamilyCheckIns_filterByWechatUserId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-checkins")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("wechat_user_id", "1")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }
    }
}
