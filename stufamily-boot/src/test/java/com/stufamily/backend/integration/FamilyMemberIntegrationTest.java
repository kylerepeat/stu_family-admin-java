package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FamilyMemberIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldListFamilyMembersWithPagination() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
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
    void shouldListFamilyMembersWithKeywordByMemberName() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("keyword", "Member")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyMembersWithKeywordByPhone() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("keyword", "1380000")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyMembersWithKeywordByGroupNo() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("keyword", "FGPOSTMAN")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldListFamilyMembersWithKeywordByOwnerNickname() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("keyword", "Postman")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());

        rateLimitWait();
    }

    @Test
    void shouldReturnEmptyResultForNonExistentKeyword() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("keyword", "NONEXISTENTKEYWORD12345")
                        .param("page_no", "1")
                        .param("page_size", "20")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(0));

        rateLimitWait();
    }

    @Test
    void shouldHandleLargePageSize() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("page_no", "1")
                        .param("page_size", "200")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageSize").exists());

        rateLimitWait();
    }

    @Test
    void shouldUseDefaultPaginationWhenNotSpecified() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/family-members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));

        rateLimitWait();
    }

    @Test
    void shouldVerifyMemberFieldsInResponse() throws Exception {
        String response = mockMvc.perform(withAuth(get("/api/admin/family-members")
                        .param("keyword", "Seed")
                        .param("page_no", "1")
                        .param("page_size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var items = objectMapper.readTree(response).path("data").path("items");
        if (items.isArray() && items.size() > 0) {
            var firstMember = items.get(0);
            assert firstMember.has("memberId");
            assert firstMember.has("memberNo");
            assert firstMember.has("memberName");
            assert firstMember.has("memberStatus");
            assert firstMember.has("groupNo");
            assert firstMember.has("ownerNickname");
        }

        rateLimitWait();
    }

    @Test
    void shouldRejectUnauthorizedFamilyMemberQuery() throws Exception {
        mockMvc.perform(get("/api/admin/family-members")
                        .param("page_no", "1")
                        .param("page_size", "10"))
                .andExpect(status().isUnauthorized());
    }
}
