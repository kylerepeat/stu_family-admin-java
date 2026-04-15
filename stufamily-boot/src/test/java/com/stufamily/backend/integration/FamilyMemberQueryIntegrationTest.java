package com.stufamily.backend.integration;

import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductDO;
import com.stufamily.backend.product.infrastructure.persistence.dataobject.ProductFamilyCardPlanDO;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductMapper;
import com.stufamily.backend.product.infrastructure.persistence.mapper.ProductFamilyCardPlanMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FamilyMemberQueryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private FamilyGroupMapper familyGroupMapper;

    @Autowired
    private FamilyMemberCardMapper familyMemberCardMapper;

    @Autowired
    private OrderMainMapper orderMainMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductFamilyCardPlanMapper productFamilyCardPlanMapper;

    @Test
    void testListFamilyMembers() throws Exception {
        Long userId = createTestWechatUser("家庭成员查询测试用户");
        Long groupId = createTestFamilyGroup(userId);
        createTestFamilyMember(groupId, userId, "张三");
        createTestFamilyMember(groupId, userId, "李四");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").isNumber())
            .andExpect(jsonPath("$.data.pageNo").value(1))
            .andExpect(jsonPath("$.data.items[0].memberNo").isString())
            .andExpect(jsonPath("$.data.items[0].memberName").isString())
            .andExpect(jsonPath("$.data.items[0].groupNo").isString())
            .andExpect(jsonPath("$.data.items[0].ownerNickname").isString());
    }

    @Test
    void testListFamilyMembersWithKeyword() throws Exception {
        Long userId = createTestWechatUser("关键字查询测试用户");
        Long groupId = createTestFamilyGroup(userId);
        createTestFamilyMember(groupId, userId, "王五关键字");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", "关键字")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.memberName=='王五关键字')]").exists());
    }

    @Test
    void testListFamilyMembersPagination() throws Exception {
        Long userId = createTestWechatUser("分页测试用户");
        Long groupId = createTestFamilyGroup(userId);
        for (int i = 1; i <= 5; i++) {
            createTestFamilyMember(groupId, userId, "成员" + i);
        }
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.pageSize").value(2))
            .andExpect(jsonPath("$.data.totalPages").isNumber());
    }

    @Test
    void testListFamilyMembersWithStudentCardNo() throws Exception {
        Long userId = createTestWechatUser("学号查询测试用户");
        Long groupId = createTestFamilyGroup(userId);
        createTestFamilyMemberWithStudentNo(groupId, userId, "学生A", "STU2024001");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", "STU2024")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.studentOrCardNo=='STU2024001')]").exists());
    }

    @Test
    void testListFamilyMembersWithPhone() throws Exception {
        Long userId = createTestWechatUser("电话查询测试用户");
        Long groupId = createTestFamilyGroup(userId);
        String phone = "13812345678";
        createTestFamilyMemberWithPhone(groupId, userId, "联系人", phone);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", "1381234")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.phone=='" + phone + "')]").exists());
    }

    @Test
    void testListFamilyMembersWithGroupNo() throws Exception {
        Long userId = createTestWechatUser("家庭组编号查询测试用户");
        String groupNo = "G" + System.currentTimeMillis();
        Long groupId = createTestFamilyGroupWithGroupNo(userId, groupNo);
        createTestFamilyMember(groupId, userId, "组成员");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", groupNo)
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.groupNo=='" + groupNo + "')]").exists());
    }

    @Test
    void testListFamilyMembersWithMemberNo() throws Exception {
        Long userId = createTestWechatUser("成员编号查询测试用户");
        Long groupId = createTestFamilyGroup(userId);
        String memberNo = "M" + System.currentTimeMillis();
        createTestFamilyMemberWithMemberNo(groupId, userId, "编号成员", memberNo);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", memberNo)
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.memberNo=='" + memberNo + "')]").exists());
    }

    @Test
    void testListFamilyMembersWithMemberStatus() throws Exception {
        Long userId = createTestWechatUser("成员状态测试用户");
        Long groupId = createTestFamilyGroup(userId);
        createTestFamilyMemberWithStatus(groupId, userId, "活跃成员", "ACTIVE");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-members")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.memberStatus=='ACTIVE')]").exists());
    }

    private Long createTestWechatUser(String nickname) {
        SysUserDO user = new SysUserDO();
        user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setUserType("WECHAT");
        user.setStatus("ACTIVE");
        user.setOpenid("openid_" + System.nanoTime() + "_" + (int)(Math.random() * 10000));
        user.setNickname(nickname);
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setPhone("138" + (int)(Math.random() * 100000000));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        sysUserMapper.insert(user);
        return user.getId();
    }

    private Long createTestFamilyGroup(Long ownerUserId) {
        String groupNo = "G" + System.currentTimeMillis();
        return createTestFamilyGroupWithGroupNo(ownerUserId, groupNo);
    }

    private Long createTestFamilyGroupWithGroupNo(Long ownerUserId, String groupNo) {
        // 先创建商品、套餐计划和订单，用于外键约束
        Long productId = createTestProduct();
        Long planId = createTestProductPlan(productId);
        Long orderId = createTestOrder(ownerUserId, productId);

        FamilyGroupDO group = new FamilyGroupDO();
        group.setGroupNo(groupNo);
        group.setSourceOrderId(orderId);
        group.setFamilyCardProductId(productId);
        group.setFamilyCardPlanId(planId);
        group.setOwnerUserId(ownerUserId);
        group.setMaxMembers(5);
        group.setCurrentMembers(1);
        group.setStatus("ACTIVE");
        group.setActivatedAt(OffsetDateTime.now());
        group.setExpireAt(OffsetDateTime.of(2027, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(8)));
        group.setCreatedAt(OffsetDateTime.now());
        group.setUpdatedAt(OffsetDateTime.now());
        familyGroupMapper.insert(group);
        return group.getId();
    }

    private Long createTestProduct() {
        // 先创建sys_user用户用于外键约束
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

        ProductDO product = new ProductDO();
        product.setProductNo("P" + System.currentTimeMillis());
        product.setProductType("FAMILY_CARD");
        product.setTitle("测试家庭卡");
        product.setSubtitle("测试副标题");
        product.setDetailContent("测试详情");
        product.setPublishStatus("DRAFT");
        product.setDeleted(false);
        product.setTop(false);
        product.setDisplayPriority(10);
        product.setSaleStartAt(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8)));
        product.setSaleEndAt(OffsetDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(8)));
        product.setCreatedBy(user.getId());
        product.setUpdatedBy(user.getId());
        productMapper.insert(product);
        return product.getId();
    }

    private Long createTestProductPlan(Long productId) {
        ProductFamilyCardPlanDO plan = new ProductFamilyCardPlanDO();
        plan.setProductId(productId);
        plan.setDurationType("MONTH");
        plan.setDurationMonths(3);
        plan.setPriceCents(9900L);
        plan.setMaxFamilyMembers(5);
        plan.setEnabled(true);
        productFamilyCardPlanMapper.insert(plan);
        return plan.getId();
    }

    private Long createTestOrder(Long buyerUserId, Long productId) {
        String orderNo = "O" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        OrderMainDO order = new OrderMainDO();
        order.setOrderNo(orderNo);
        order.setBuyerUserId(buyerUserId);
        order.setOrderType("FAMILY_CARD");
        order.setOrderStatus("PAID");
        order.setTotalAmountCents(10000L);
        order.setDiscountAmountCents(0L);
        order.setPayableAmountCents(10000L);
        order.setCurrency("CNY");
        order.setSourceChannel("WECHAT_MINI");
        order.setExpireAt(OffsetDateTime.now().plusHours(24));
        order.setPaidAt(OffsetDateTime.now());
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        orderMainMapper.insert(order);
        return order.getId();
    }

    private void createTestFamilyMember(Long groupId, Long addedByUserId, String memberName) {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(groupId);
        member.setMemberNo("M" + System.currentTimeMillis() + (int)(Math.random() * 1000));
        member.setMemberName(memberName);
        member.setStudentOrCardNo("S" + System.currentTimeMillis());
        member.setPhone("138" + (int)(Math.random() * 100000000));
        member.setAddedByUserId(addedByUserId);
        member.setStatus("ACTIVE");
        member.setCardReceivedDate(LocalDate.now());
        member.setJoinedAt(OffsetDateTime.now());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        familyMemberCardMapper.insert(member);
    }

    private void createTestFamilyMemberWithMemberNo(Long groupId, Long addedByUserId, String memberName, String memberNo) {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(groupId);
        member.setMemberNo(memberNo);
        member.setMemberName(memberName);
        member.setStudentOrCardNo("S" + System.currentTimeMillis());
        member.setPhone("138" + (int)(Math.random() * 100000000));
        member.setAddedByUserId(addedByUserId);
        member.setStatus("ACTIVE");
        member.setCardReceivedDate(LocalDate.now());
        member.setJoinedAt(OffsetDateTime.now());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        familyMemberCardMapper.insert(member);
    }

    private void createTestFamilyMemberWithStudentNo(Long groupId, Long addedByUserId, String memberName, String studentNo) {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(groupId);
        member.setMemberNo("M" + System.currentTimeMillis());
        member.setMemberName(memberName);
        member.setStudentOrCardNo(studentNo);
        member.setPhone("138" + (int)(Math.random() * 100000000));
        member.setAddedByUserId(addedByUserId);
        member.setStatus("ACTIVE");
        member.setCardReceivedDate(LocalDate.now());
        member.setJoinedAt(OffsetDateTime.now());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        familyMemberCardMapper.insert(member);
    }

    private void createTestFamilyMemberWithPhone(Long groupId, Long addedByUserId, String memberName, String phone) {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(groupId);
        member.setMemberNo("M" + System.currentTimeMillis());
        member.setMemberName(memberName);
        member.setStudentOrCardNo("S" + System.currentTimeMillis());
        member.setPhone(phone);
        member.setAddedByUserId(addedByUserId);
        member.setStatus("ACTIVE");
        member.setCardReceivedDate(LocalDate.now());
        member.setJoinedAt(OffsetDateTime.now());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        familyMemberCardMapper.insert(member);
    }

    private void createTestFamilyMemberWithStatus(Long groupId, Long addedByUserId, String memberName, String status) {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(groupId);
        member.setMemberNo("M" + System.currentTimeMillis());
        member.setMemberName(memberName);
        member.setStudentOrCardNo("S" + System.currentTimeMillis());
        member.setPhone("138" + (int)(Math.random() * 100000000));
        member.setAddedByUserId(addedByUserId);
        member.setStatus(status);
        member.setCardReceivedDate(LocalDate.now());
        member.setJoinedAt(OffsetDateTime.now());
        member.setCreatedAt(OffsetDateTime.now());
        member.setUpdatedAt(OffsetDateTime.now());
        familyMemberCardMapper.insert(member);
    }
}
