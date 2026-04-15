package com.stufamily.backend.integration;

import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductValueAddedSkuDO;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductFamilyCardPlanMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductValueAddedSkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductFamilyCardPlanMapper productFamilyCardPlanMapper;

    @Autowired
    private ProductValueAddedSkuMapper productValueAddedSkuMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void testListProducts() throws Exception {
        createTestProduct("FAMILY_CARD", "测试家庭卡商品", "ON_SHELF");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/products")
                .header("Authorization", getAuthorizationHeader())
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").isNumber())
            .andExpect(jsonPath("$.data.pageNo").value(1));
    }

    @Test
    void testListProductsWithPublishStatusFilter() throws Exception {
        createTestProduct("FAMILY_CARD", "上架商品", "ON_SHELF");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/products")
                .header("Authorization", getAuthorizationHeader())
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31")
                .param("publish_status", "ON_SHELF")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.publishStatus=='ON_SHELF')]").exists());
    }

    @Test
    void testGetProductDetail() throws Exception {
        Long productId = createTestProductWithPlan("FAMILY_CARD", "详情测试商品");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/products/{productId}", productId)
                .header("Authorization", getAuthorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.productNo").isString())
            .andExpect(jsonPath("$.data.productType").value("FAMILY_CARD"))
            .andExpect(jsonPath("$.data.title").value("详情测试商品"))
            .andExpect(jsonPath("$.data.familyCardPlans").isArray());
    }

    @Test
    void testCreateFamilyCardProduct() throws Exception {
        String requestBody = """
            {
                "productType": "FAMILY_CARD",
                "title": "新建家庭卡商品",
                "subtitle": "副标题",
                "detailContent": "商品详情内容",
                "imageUrls": ["http://example.com/image1.jpg"],
                "contactName": "联系人",
                "contactPhone": "13800138000",
                "publishStatus": "DRAFT",
                "top": false,
                "displayPriority": 10,
                "familyCardPlans": [
                    {
                        "durationType": "MONTH",
                        "durationMonths": 3,
                        "priceCents": 9900,
                        "maxFamilyMembers": 5,
                        "enabled": true
                    }
                ]
            }
            """;
        sleepForRateLimit();

        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.productType").value("FAMILY_CARD"))
            .andExpect(jsonPath("$.data.title").value("新建家庭卡商品"))
            .andExpect(jsonPath("$.data.publishStatus").value("DRAFT"))
            .andExpect(jsonPath("$.data.familyCardPlans[0].durationType").value("MONTH"))
            .andExpect(jsonPath("$.data.familyCardPlans[0].priceCents").value(9900));
    }

    @Test
    void testCreateValueAddedServiceProduct() throws Exception {
        String requestBody = """
            {
                "productType": "VALUE_ADDED_SERVICE",
                "title": "新建增值服务商品",
                "subtitle": "增值服务副标题",
                "detailContent": "增值服务详情",
                "imageUrls": ["http://example.com/image2.jpg"],
                "contactName": "服务联系人",
                "contactPhone": "13900139000",
                "publishStatus": "DRAFT",
                "top": true,
                "displayPriority": 20,
                "valueAddedSkus": [
                    {
                        "title": "增值服务SKU1",
                        "priceCents": 19900,
                        "enabled": true
                    }
                ]
            }
            """;
        sleepForRateLimit();

        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.productType").value("VALUE_ADDED_SERVICE"))
            .andExpect(jsonPath("$.data.title").value("新建增值服务商品"))
            .andExpect(jsonPath("$.data.valueAddedSkus[0].title").value("增值服务SKU1"))
            .andExpect(jsonPath("$.data.valueAddedSkus[0].priceCents").value(19900));
    }

    @Test
    void testUpdateProduct() throws Exception {
        Long productId = createTestProduct("FAMILY_CARD", "待更新商品", "DRAFT");
        sleepForRateLimit();

        String requestBody = """
            {
                "productType": "FAMILY_CARD",
                "title": "已更新商品标题",
                "subtitle": "更新后的副标题",
                "detailContent": "更新后的详情",
                "imageUrls": ["http://example.com/updated.jpg"],
                "contactName": "新联系人",
                "contactPhone": "13700137000",
                "publishStatus": "DRAFT",
                "top": true,
                "displayPriority": 50
            }
            """;

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.title").value("已更新商品标题"))
            .andExpect(jsonPath("$.data.subtitle").value("更新后的副标题"))
            .andExpect(jsonPath("$.data.top").value(true));
    }

    @Test
    void testOnShelfProduct() throws Exception {
        Long productId = createTestProduct("FAMILY_CARD", "待上架商品", "DRAFT");
        sleepForRateLimit();

        mockMvc.perform(post("/api/admin/products/{productId}/on-shelf", productId)
                .header("Authorization", getAuthorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.publishStatus").value("ON_SHELF"));
    }

    @Test
    void testOffShelfProduct() throws Exception {
        Long productId = createTestProduct("FAMILY_CARD", "待下架商品", "ON_SHELF");
        sleepForRateLimit();

        mockMvc.perform(post("/api/admin/products/{productId}/off-shelf", productId)
                .header("Authorization", getAuthorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.publishStatus").value("OFF_SHELF"));
    }

    @Test
    void testCreateProductValidationError() throws Exception {
        String requestBody = """
            {
                "productType": "FAMILY_CARD",
                "title": "",
                "detailContent": "详情"
            }
            """;
        sleepForRateLimit();

        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                .param("sale_start_at", "2026-01-01")
                .param("sale_end_at", "2026-12-31"))
            .andExpect(status().isUnauthorized());
    }

    private Long createTestProduct(String productType, String title, String publishStatus) {
        // 先创建一个sys_user用户用于外键约束
        Long sysUserId = createTestSysUser();
        
        ProductDO product = new ProductDO();
        product.setProductNo("P" + System.currentTimeMillis());
        product.setProductType(productType);
        product.setTitle(title);
        product.setSubtitle("测试副标题");
        product.setDetailContent("测试详情");
        product.setPublishStatus(publishStatus);
        product.setDeleted(false);
        product.setTop(false);
        product.setDisplayPriority(10);
        product.setSaleStartAt(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8)));
        product.setSaleEndAt(OffsetDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(8)));
        product.setCreatedBy(sysUserId);
        product.setUpdatedBy(sysUserId);
        productMapper.insert(product);
        return product.getId();
    }
    
    private Long createTestSysUser() {
        SysUserDO user = new SysUserDO();
        user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setUserType("WECHAT");
        user.setStatus("ACTIVE");
        user.setOpenid("openid_" + System.nanoTime() + "_" + (int)(Math.random() * 10000));
        user.setNickname("测试用户");
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setPhone("138" + (int)(Math.random() * 100000000));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        sysUserMapper.insert(user);
        return user.getId();
    }

    private Long createTestProductWithPlan(String productType, String title) {
        Long productId = createTestProduct(productType, title, "DRAFT");

        ProductFamilyCardPlanDO plan = new ProductFamilyCardPlanDO();
        plan.setProductId(productId);
        plan.setDurationType("MONTH");
        plan.setDurationMonths(3);
        plan.setPriceCents(9900L);
        plan.setMaxFamilyMembers(5);
        plan.setEnabled(true);
        productFamilyCardPlanMapper.insert(plan);

        return productId;
    }
}
