package com.stufamily.backend.integration;

import com.stufamily.backend.adminapi.request.AdminProductUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("商品管理集成测试")
public class ProductManagementIntegrationTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/admin/products";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private OffsetDateTime nowOffset() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8));
    }

    private OffsetDateTime plusYearsOffset(int years) {
        return nowOffset().plusYears(years);
    }

    @Nested
    @DisplayName("商品列表查询")
    class ListProductsTests {

        @Test
        @DisplayName("查询商品列表-成功返回分页数据")
        void listProducts_success() throws Exception {
            String today = LocalDateTime.now().format(DATE_FORMATTER);
            String nextYear = LocalDateTime.now().plusYears(1).format(DATE_FORMATTER);

            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("sale_start_at", today)
                    .param("sale_end_at", nextYear)
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
        @DisplayName("查询商品列表-按发布状态筛选")
        void listProducts_filterByPublishStatus() throws Exception {
            String today = LocalDateTime.now().format(DATE_FORMATTER);
            String nextYear = LocalDateTime.now().plusYears(1).format(DATE_FORMATTER);

            mockMvc.perform(get(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .param("sale_start_at", today)
                    .param("sale_end_at", nextYear)
                    .param("publish_status", "ON_SHELF")
                    .param("page_no", "1")
                    .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
        }

        @Test
        @DisplayName("查询商品列表-未授权返回401")
        void listProducts_unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .param("sale_start_at", "2024-01-01")
                    .param("sale_end_at", "2024-12-31"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("商品详情查询")
    class GetProductDetailTests {

        @Test
        @DisplayName("查询商品详情-成功返回完整信息")
        void getProductDetail_success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/1")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productType").exists())
                .andExpect(jsonPath("$.data.title").isString())
                .andExpect(jsonPath("$.data.publishStatus").exists());
        }

        @Test
        @DisplayName("查询商品详情-商品不存在返回错误")
        void getProductDetail_notFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999999")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        }
    }

    @Nested
    @DisplayName("创建商品")
    class CreateProductTests {

        @Test
        @DisplayName("创建家庭卡商品-成功")
        void createFamilyCardProduct_success() throws Exception {
            waitForReplayGuard();

            AdminProductUpdateRequest request = new AdminProductUpdateRequest(
                "FAMILY_CARD",
                "Integration Test Family Card " + System.currentTimeMillis(),
                "Test Subtitle",
                "Test detail content for integration test",
                List.of("https://example.com/test.png"),
                "Test Contact",
                "13800000000",
                nowOffset(),
                plusYearsOffset(1),
                nowOffset(),
                plusYearsOffset(1),
                "DRAFT",
                false,
                1,
                List.of(
                    new AdminProductUpdateRequest.FamilyCardPlanRequest(null, "MONTH", 1, 19900L, 3, true),
                    new AdminProductUpdateRequest.FamilyCardPlanRequest(null, "SEMESTER", 6, 99900L, 5, true)
                ),
                null
            );

            mockMvc.perform(post(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.productType").value("FAMILY_CARD"))
                .andExpect(jsonPath("$.data.title").value(request.title()))
                .andExpect(jsonPath("$.data.familyCardPlans", hasSize(2)));
        }

        @Test
        @DisplayName("创建增值服务商品-成功")
        void createValueAddedProduct_success() throws Exception {
            waitForReplayGuard();

            AdminProductUpdateRequest request = new AdminProductUpdateRequest(
                "VALUE_ADDED_SERVICE",
                "Integration Test VAS " + System.currentTimeMillis(),
                "Test VAS Subtitle",
                "Test VAS detail content",
                List.of("https://example.com/vas.png"),
                "VAS Contact",
                "13900000000",
                nowOffset(),
                plusYearsOffset(1),
                nowOffset(),
                plusYearsOffset(1),
                "DRAFT",
                false,
                1,
                null,
                List.of(
                    new AdminProductUpdateRequest.ValueAddedSkuRequest(null, "Basic SKU", 9900L, true),
                    new AdminProductUpdateRequest.ValueAddedSkuRequest(null, "Premium SKU", 19900L, true)
                )
            );

            mockMvc.perform(post(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productType").value("VALUE_ADDED_SERVICE"))
                .andExpect(jsonPath("$.data.valueAddedSkus", hasSize(2)));
        }

        @Test
        @DisplayName("创建商品-缺少必填字段返回错误")
        void createProduct_missingRequiredField() throws Exception {
            waitForReplayGuard();

            String invalidRequest = "{\"productType\":\"FAMILY_CARD\"}";

            mockMvc.perform(post(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("编辑商品")
    class UpdateProductTests {

        @Test
        @DisplayName("编辑商品-成功更新标题")
        void updateProduct_success() throws Exception {
            waitForReplayGuard();

            AdminProductUpdateRequest createRequest = new AdminProductUpdateRequest(
                "FAMILY_CARD",
                "Product To Update " + System.currentTimeMillis(),
                null,
                "Content for update test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "DRAFT",
                false,
                0,
                List.of(new AdminProductUpdateRequest.FamilyCardPlanRequest(null, "MONTH", 1, 19900L, 3, true)),
                null
            );

            MvcResult createResult = mockMvc.perform(post(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

            Long productId = extractIdFromData(createResult);
            
            waitForReplayGuard();

            AdminProductUpdateRequest updateRequest = new AdminProductUpdateRequest(
                "FAMILY_CARD",
                "Updated Product Title " + System.currentTimeMillis(),
                "Updated Subtitle",
                "Updated content",
                List.of("https://example.com/updated.png"),
                "Updated Contact",
                "13700000000",
                null,
                null,
                null,
                null,
                "DRAFT",
                true,
                100,
                List.of(new AdminProductUpdateRequest.FamilyCardPlanRequest(null, "YEAR", 12, 179900L, 8, true)),
                null
            );

            mockMvc.perform(put(BASE_URL + "/" + productId)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(updateRequest.title()));
        }
    }

    @Nested
    @DisplayName("商品上下架")
    class ProductShelfTests {

        @Test
        @DisplayName("商品上架-成功")
        void onShelf_success() throws Exception {
            waitForReplayGuard();

            AdminProductUpdateRequest createRequest = new AdminProductUpdateRequest(
                "FAMILY_CARD",
                "Product For Shelf Test " + System.currentTimeMillis(),
                null,
                "Content for shelf test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "DRAFT",
                false,
                0,
                List.of(new AdminProductUpdateRequest.FamilyCardPlanRequest(null, "MONTH", 1, 19900L, 3, true)),
                null
            );

            MvcResult createResult = mockMvc.perform(post(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

            Long productId = extractIdFromData(createResult);
            
            waitForReplayGuard();

            mockMvc.perform(post(BASE_URL + "/" + productId + "/on-shelf")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publishStatus").value("ON_SHELF"));
        }

        @Test
        @DisplayName("商品下架-成功")
        void offShelf_success() throws Exception {
            waitForReplayGuard();

            AdminProductUpdateRequest createRequest = new AdminProductUpdateRequest(
                "VALUE_ADDED_SERVICE",
                "Product For OffShelf Test " + System.currentTimeMillis(),
                null,
                "Content for offshelf test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "ON_SHELF",
                false,
                0,
                null,
                List.of(new AdminProductUpdateRequest.ValueAddedSkuRequest(null, "Test SKU", 9900L, true))
            );

            MvcResult createResult = mockMvc.perform(post(BASE_URL)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

            Long productId = extractIdFromData(createResult);
            
            waitForReplayGuard();

            mockMvc.perform(post(BASE_URL + "/" + productId + "/off-shelf")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publishStatus").value("OFF_SHELF"));
        }
    }
}
