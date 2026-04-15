package com.stufamily.backend.integration;

import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyGroupDO;
import com.stufamily.backend.family.infrastructure.persistence.dataobject.FamilyMemberCardDO;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyGroupMapper;
import com.stufamily.backend.family.infrastructure.persistence.mapper.FamilyMemberCardMapper;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentRefundDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentTransactionDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.ServiceReviewDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentRefundMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentTransactionMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.ServiceReviewMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderRefundAndFamilyGroupIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private OrderMainMapper orderMainMapper;

    @Autowired
    private PaymentTransactionMapper paymentTransactionMapper;

    @Autowired
    private PaymentRefundMapper paymentRefundMapper;

    @Autowired
    private FamilyGroupMapper familyGroupMapper;

    @Autowired
    private FamilyMemberCardMapper familyMemberCardMapper;

    @Autowired
    private ServiceReviewMapper serviceReviewMapper;

    @Test
    void testRefundOrder() throws Exception {
        Long userId = createTestWechatUser("退款测试用户");
        String orderNo = createPaidOrder(userId, 19900L);
        createPaymentTransaction(orderNo, 19900L);
        sleepForRateLimit();

        String requestBody = """
            {
                "refundAmountCents": 9900,
                "reason": "测试退款"
            }
            """;

        mockMvc.perform(post("/api/admin/orders/{orderNo}/refund", orderNo)
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderNo").value(orderNo))
            .andExpect(jsonPath("$.data.refundAmountCents").value(9900))
            .andExpect(jsonPath("$.data.refundStatus").exists());
    }

    @Test
    void testRefundOrderWithFullAmount() throws Exception {
        Long userId = createTestWechatUser("全额退款测试用户");
        String orderNo = createPaidOrder(userId, 5000L);
        createPaymentTransaction(orderNo, 5000L);
        sleepForRateLimit();

        String requestBody = """
            {
                "refundAmountCents": 5000,
                "reason": "全额退款测试"
            }
            """;

        mockMvc.perform(post("/api/admin/orders/{orderNo}/refund", orderNo)
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.refundAmountCents").value(5000));
    }

    @Test
    void testListOrderRefunds() throws Exception {
        Long userId = createTestWechatUser("查询退款测试用户");
        String orderNo = createPaidOrder(userId, 29900L);
        createPaymentTransaction(orderNo, 29900L);
        createRefundRecord(orderNo, 9900L, "SUCCESS");
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/{orderNo}/refunds", orderNo)
                .header("Authorization", getAuthorizationHeader())
                .param("page_no", "1")
                .param("page_size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.items[0].refundNo").exists());
    }

    @Test
    void testDisableFamilyGroup() throws Exception {
        Long userId = createTestWechatUser("停用家庭组测试用户");
        String orderNo = createPaidOrder(userId, 19900L);
        Long orderId = getOrderIdByOrderNo(orderNo);
        createFamilyGroupWithMembers(orderId, userId);
        sleepForRateLimit();

        mockMvc.perform(post("/api/admin/orders/{orderNo}/disable-family-group", orderNo)
                .header("Authorization", getAuthorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderNo").value(orderNo))
            .andExpect(jsonPath("$.data.groupStatus").value("CLOSED"))
            .andExpect(jsonPath("$.data.totalMemberCount").isNumber())
            .andExpect(jsonPath("$.data.disabledMemberCount").isNumber());
    }

    @Test
    void testGetProductReviewByOrderId() throws Exception {
        Long userId = createTestWechatUser("评价查询测试用户");
        String orderNo = createPaidOrder(userId, 9900L);
        Long orderId = getOrderIdByOrderNo(orderNo);
        createProductReview(orderId, userId);
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/{orderId}/product-review", orderId)
                .header("Authorization", getAuthorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderId").value(orderId))
            .andExpect(jsonPath("$.data.stars").isNumber())
            .andExpect(jsonPath("$.data.content").isString());
    }

    @Test
    void testRefundValidationError() throws Exception {
        Long userId = createTestWechatUser("退款验证测试用户");
        String orderNo = createPaidOrder(userId, 1000L);
        sleepForRateLimit();

        String requestBody = """
            {
                "refundAmountCents": 0,
                "reason": ""
            }
            """;

        mockMvc.perform(post("/api/admin/orders/{orderNo}/refund", orderNo)
                .header("Authorization", getAuthorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
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

    private String createPaidOrder(Long buyerUserId, Long amountCents) {
        String orderNo = "O" + System.currentTimeMillis();
        OrderMainDO order = new OrderMainDO();
        order.setOrderNo(orderNo);
        order.setBuyerUserId(buyerUserId);
        order.setOrderType("FAMILY_CARD");
        order.setOrderStatus("PAID");
        order.setTotalAmountCents(amountCents);
        order.setDiscountAmountCents(0L);
        order.setPayableAmountCents(amountCents);
        order.setCurrency("CNY");
        order.setSourceChannel("WECHAT_MINI");
        order.setExpireAt(OffsetDateTime.now().plusHours(24));
        order.setPaidAt(OffsetDateTime.now());
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        orderMainMapper.insert(order);
        return orderNo;
    }

    private Long getOrderIdByOrderNo(String orderNo) {
        OrderMainDO order = orderMainMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderMainDO>()
                .eq(OrderMainDO::getOrderNo, orderNo)
        );
        return order != null ? order.getId() : null;
    }

    private void createPaymentTransaction(String orderNo, Long amountCents) {
        Long orderId = getOrderIdByOrderNo(orderNo);
        PaymentTransactionDO transaction = new PaymentTransactionDO();
        transaction.setPaymentNo("PAY" + System.currentTimeMillis());
        transaction.setOrderId(orderId);
        transaction.setPaymentStatus("SUCCESS");
        transaction.setChannel("WECHAT_PAY");
        transaction.setOutTradeNo(orderNo);
        transaction.setTotalAmountCents(amountCents);
        transaction.setCurrency("CNY");
        transaction.setSuccessTime(OffsetDateTime.now());
        transaction.setCreatedAt(OffsetDateTime.now());
        transaction.setUpdatedAt(OffsetDateTime.now());
        paymentTransactionMapper.insert(transaction);
    }

    private void createRefundRecord(String orderNo, Long refundAmountCents, String status) {
        PaymentRefundDO refund = new PaymentRefundDO();
        refund.setRefundNo("R" + System.currentTimeMillis());
        refund.setRefundAmountCents(refundAmountCents);
        refund.setReason("测试退款");
        refund.setRefundStatus(status);
        refund.setWechatRefundId("wx_refund_" + System.currentTimeMillis());
        refund.setSuccessTime(OffsetDateTime.now());
        refund.setCreatedAt(OffsetDateTime.now());
        refund.setUpdatedAt(OffsetDateTime.now());
        paymentRefundMapper.insert(refund);
    }

    private Long createFamilyGroupWithMembers(Long orderId, Long ownerUserId) {
        FamilyGroupDO group = new FamilyGroupDO();
        group.setGroupNo("G" + System.currentTimeMillis());
        group.setSourceOrderId(orderId);
        group.setOwnerUserId(ownerUserId);
        group.setMaxMembers(5);
        group.setCurrentMembers(2);
        group.setStatus("ACTIVE");
        group.setActivatedAt(OffsetDateTime.now());
        group.setExpireAt(OffsetDateTime.of(2027, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(8)));
        group.setCreatedAt(OffsetDateTime.now());
        group.setUpdatedAt(OffsetDateTime.now());
        familyGroupMapper.insert(group);

        createFamilyMember(group.getId(), ownerUserId, "成员1");
        createFamilyMember(group.getId(), ownerUserId, "成员2");

        return group.getId();
    }

    private void createFamilyMember(Long groupId, Long addedByUserId, String memberName) {
        FamilyMemberCardDO member = new FamilyMemberCardDO();
        member.setGroupId(groupId);
        member.setMemberNo("M" + System.currentTimeMillis());
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

    private void createProductReview(Long orderId, Long buyerUserId) {
        ServiceReviewDO review = new ServiceReviewDO();
        review.setOrderId(orderId);
        review.setBuyerUserId(buyerUserId);
        review.setProductId(1L);
        review.setProductType("FAMILY_CARD");
        review.setStars(5);
        review.setContent("测试评价内容，非常满意！");
        review.setCreatedAt(OffsetDateTime.now());
        review.setUpdatedAt(OffsetDateTime.now());
        serviceReviewMapper.insert(review);
    }
}
