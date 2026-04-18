package com.stufamily.backend.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("家庭成员查询集成测试")
public class FamilyMemberQueryIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/admin";

    @Nested
    @DisplayName("家庭成员列表查询")
    class FamilyMemberQueryTests {

        @Test
        @DisplayName("查询家庭成员列表-成功返回分页数据")
        void listFamilyMembers_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-members")
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
        @DisplayName("查询家庭成员列表-按关键词搜索")
        void listFamilyMembers_filterByKeyword() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-members")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("keyword", "test")
                    .param("page_no", "1")
                    .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询家庭成员列表-未授权返回401")
        void listFamilyMembers_unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-members")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("查询家庭成员列表-验证返回字段")
        void listFamilyMembers_verifyResponseFields() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-members")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("打卡记录查询")
    class FamilyCheckInQueryTests {

        @Test
        @DisplayName("查询打卡记录-成功返回分页数据")
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
        @DisplayName("查询打卡记录-按家庭成员ID筛选")
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
        @DisplayName("查询打卡记录-按微信用户ID筛选")
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

        @Test
        @DisplayName("查询打卡记录-验证返回字段")
        void listFamilyCheckIns_verifyResponseFields() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-checkins")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }
    }

    @Nested
    @DisplayName("家庭卡查询")
    class FamilyCardQueryTests {

        @Test
        @DisplayName("查询家庭卡列表-验证成员数量字段")
        void listFamilyCards_verifyMemberCountFields() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-cards")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("status", "ACTIVE")
                    .param("page_no", "1")
                    .param("page_size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].maxMembers").isNumber())
                .andExpect(jsonPath("$.data.items[0].currentMembers").isNumber())
                .andExpect(jsonPath("$.data.items[0].status").exists());
        }

        @Test
        @DisplayName("查询家庭卡列表-验证时间字段")
        void listFamilyCards_verifyTimeFields() throws Exception {
            mockMvc.perform(get(BASE_URL + "/family-cards")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("page_no", "1")
                    .param("page_size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].activatedAt").exists())
                .andExpect(jsonPath("$.data.items[0].expireAt").exists());
        }
    }
}
