package com.stufamily.backend.integration;

import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyCheckInDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyCheckInMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import java.math.BigDecimal;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserAndOrderQueryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private OrderMainMapper orderMainMapper;

    @Autowired
    private FamilyGroupMapper familyGroupMapper;

    @Autowired
    private FamilyCheckInMapper familyCheckInMapper;

    @Test
    void testGetFilterOptions() throws Exception {
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/filter-options")
                .header("Authorization", getAuthorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.productPublishStatuses").isArray())
            .andExpect(jsonPath("$.data.weixinUserStatuses").isArray())
            .andExpect(jsonPath("$.data.orderStatuses").isArray())
            .andExpect(jsonPath("$.data.orderTypes").isArray())
            .andExpect(jsonPath("$.data.familyCardStatuses").isArray());
    }

    @Test
    void testListWechatUsers() throws Exception {
        createTestWechatUser("测试用户1", "ACTIVE");
        createTestWechatUser("测试用户2", "ACTIVE");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/weixin-users")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").isNumber())
            .andExpect(jsonPath("$.data.pageNo").value(1));
    }

    @Test
    void testListWechatUsersWithKeyword() throws Exception {
        createTestWechatUser("特殊昵称关键字", "ACTIVE");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/weixin-users")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", "特殊昵称")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.nickname=='特殊昵称关键字')]").exists());
    }

    @Test
    void testListWechatUsersWithStatusFilter() throws Exception {
        createTestWechatUser("正常用户", "ACTIVE");
        createTestWechatUser("锁定用户", "LOCKED");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/weixin-users")
                .header("Authorization", getAuthorizationHeader())
                .param("status", "ACTIVE")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.status=='ACTIVE')]").exists());
    }

    @Test
    void testListOrders() throws Exception {
        Long userId = createTestWechatUser("订单测试用户", "ACTIVE");
        createTestOrder(userId, "FAMILY_CARD", "PAID", 9900L);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.total").isNumber())
            .andExpect(jsonPath("$.data.items[0].buyerUserId").isNumber())
            .andExpect(jsonPath("$.data.items[0].buyerNickname").isString());
    }

    @Test
    void testListOrdersWithStatusFilter() throws Exception {
        Long userId = createTestWechatUser("订单状态测试用户", "ACTIVE");
        createTestOrder(userId, "FAMILY_CARD", "PAID", 19900L);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders")
                .header("Authorization", getAuthorizationHeader())
                .param("order_status", "PAID")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.orderStatus=='PAID')]").exists());
    }

    @Test
    void testListOrdersWithTypeFilter() throws Exception {
        Long userId = createTestWechatUser("订单类型测试用户", "ACTIVE");
        createTestOrder(userId, "VALUE_ADDED_SERVICE", "PAID", 5000L);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders")
                .header("Authorization", getAuthorizationHeader())
                .param("order_type", "VALUE_ADDED_SERVICE")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.orderType=='VALUE_ADDED_SERVICE')]").exists());
    }

    @Test
    void testListOrdersWithKeyword() throws Exception {
        Long userId = createTestWechatUser("关键字测试用户", "ACTIVE");
        String orderNo = "O" + System.currentTimeMillis();
        createTestOrderWithOrderNo(userId, "FAMILY_CARD", "PAID", 9900L, orderNo);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders")
                .header("Authorization", getAuthorizationHeader())
                .param("keyword", orderNo)
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.orderNo=='" + orderNo + "')]").exists());
    }

    @Test
    void testListFamilyCards() throws Exception {
        Long userId = createTestWechatUser("家庭卡测试用户", "ACTIVE");
        createTestFamilyGroup(userId, "ACTIVE");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-cards")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.items[0].groupNo").isString())
            .andExpect(jsonPath("$.data.items[0].ownerNickname").isString());
    }

    @Test
    void testListFamilyCardsWithStatusFilter() throws Exception {
        Long userId = createTestWechatUser("家庭卡状态测试用户", "ACTIVE");
        createTestFamilyGroup(userId, "ACTIVE");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-cards")
                .header("Authorization", getAuthorizationHeader())
                .param("status", "ACTIVE")
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.status=='ACTIVE')]").exists());
    }

    @Test
    void testListFamilyCheckIns() throws Exception {
        Long userId = createTestWechatUser("打卡测试用户", "ACTIVE");
        Long groupId = createTestFamilyGroup(userId, "ACTIVE");
        createTestFamilyCheckIn(groupId, userId);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-checkins")
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.items[0].checkinNo").isString());
    }

    @Test
    void testListFamilyCheckInsWithWechatUserId() throws Exception {
        Long userId = createTestWechatUser("打卡筛选测试用户", "ACTIVE");
        Long groupId = createTestFamilyGroup(userId, "ACTIVE");
        createTestFamilyCheckIn(groupId, userId);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/family-checkins")
                .header("Authorization", getAuthorizationHeader())
                .param("wechat_user_id", userId.toString())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[?(@.ownerUserId==" + userId + ")]").exists());
    }

    private Long createTestWechatUser(String nickname, String status) {
        SysUserDO user = new SysUserDO();
        user.setUserNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setUserType("WECHAT");
        user.setStatus(status);
        user.setOpenid("openid_" + System.nanoTime() + "_" + (int)(Math.random() * 10000));
        user.setNickname(nickname);
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setPhone("138" + (int)(Math.random() * 100000000));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        sysUserMapper.insert(user);
        return user.getId();
    }

    private String createTestOrder(Long buyerUserId, String orderType, String orderStatus, Long amountCents) {
        String orderNo = "O" + System.currentTimeMillis();
        return createTestOrderWithOrderNo(buyerUserId, orderType, orderStatus, amountCents, orderNo);
    }

    private String createTestOrderWithOrderNo(Long buyerUserId, String orderType, String orderStatus, Long amountCents, String orderNo) {
        OrderMainDO order = new OrderMainDO();
        order.setOrderNo(orderNo);
        order.setBuyerUserId(buyerUserId);
        order.setOrderType(orderType);
        order.setOrderStatus(orderStatus);
        order.setTotalAmountCents(amountCents);
        order.setDiscountAmountCents(0L);
        order.setPayableAmountCents(amountCents);
        order.setCurrency("CNY");
        order.setSourceChannel("WECHAT_MINI");
        order.setExpireAt(OffsetDateTime.now().plusHours(24));
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        if ("PAID".equals(orderStatus)) {
            order.setPaidAt(OffsetDateTime.now());
        }
        orderMainMapper.insert(order);
        return orderNo;
    }

    private Long createTestFamilyGroup(Long ownerUserId, String status) {
        FamilyGroupDO group = new FamilyGroupDO();
        group.setGroupNo("G" + System.currentTimeMillis());
        group.setOwnerUserId(ownerUserId);
        group.setMaxMembers(5);
        group.setCurrentMembers(1);
        group.setStatus(status);
        group.setActivatedAt(OffsetDateTime.now());
        group.setExpireAt(OffsetDateTime.of(2027, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(8)));
        group.setCreatedAt(OffsetDateTime.now());
        group.setUpdatedAt(OffsetDateTime.now());
        familyGroupMapper.insert(group);
        return group.getId();
    }

    private void createTestFamilyCheckIn(Long groupId, Long ownerUserId) {
        FamilyCheckInDO checkIn = new FamilyCheckInDO();
        checkIn.setCheckinNo("C" + System.currentTimeMillis());
        checkIn.setGroupId(groupId);
        checkIn.setOwnerUserId(ownerUserId);
        checkIn.setLatitude(new BigDecimal("31.2304"));
        checkIn.setLongitude(new BigDecimal("121.4737"));
        checkIn.setAddressText("上海市测试地址");
        checkIn.setCheckedInAt(OffsetDateTime.now());
        checkIn.setCreatedAt(OffsetDateTime.now());
        familyCheckInMapper.insert(checkIn);
    }
}
