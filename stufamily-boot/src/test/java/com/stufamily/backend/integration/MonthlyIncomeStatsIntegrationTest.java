package com.stufamily.backend.integration;

import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysUserMapper;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.OrderMainDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentRefundDO;
import com.stufamily.backend.order.infrastructure.persistence.dataobject.PaymentTransactionDO;
import com.stufamily.backend.order.infrastructure.persistence.mapper.OrderMainMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentRefundMapper;
import com.stufamily.backend.order.infrastructure.persistence.mapper.PaymentTransactionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MonthlyIncomeStatsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private OrderMainMapper orderMainMapper;

    @Autowired
    private PaymentRefundMapper paymentRefundMapper;

    @Autowired
    private PaymentTransactionMapper paymentTransactionMapper;

    @Test
    void testMonthlyIncomeStats() throws Exception {
        Long userId = createTestWechatUser("收入统计测试用户");
        createPaidOrder(userId, "FAMILY_CARD", 19900L, YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        sleepForRateLimit();

        String currentMonth = YearMonth.now().toString();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", currentMonth)
                .param("end_month", currentMonth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray())
            .andExpect(jsonPath("$.data.monthlyRefundIncome").isArray())
            .andExpect(jsonPath("$.data.totalIncomeCents").isNumber())
            .andExpect(jsonPath("$.data.totalRefundCents").isNumber())
            .andExpect(jsonPath("$.data.netIncomeCents").isNumber());
    }

    @Test
    void testMonthlyIncomeStatsWithProductType() throws Exception {
        Long userId = createTestWechatUser("产品类型统计测试用户");
        createPaidOrder(userId, "FAMILY_CARD", 29900L, YearMonth.now().atDay(5).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        createPaidOrder(userId, "VALUE_ADDED_SERVICE", 9900L, YearMonth.now().atDay(10).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        sleepForRateLimit();

        String currentMonth = YearMonth.now().toString();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", currentMonth)
                .param("end_month", currentMonth)
                .param("product_type", "FAMILY_CARD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.monthlyTotalIncome").isArray());
    }

    @Test
    void testMonthlyIncomeStatsWithDateRange() throws Exception {
        Long userId = createTestWechatUser("日期范围统计测试用户");
        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        createPaidOrder(userId, "FAMILY_CARD", 15000L, lastMonth.atDay(15).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        createPaidOrder(userId, "FAMILY_CARD", 20000L, currentMonth.atDay(15).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", lastMonth.toString())
                .param("end_month", currentMonth.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.monthlyTotalIncome.length()").value(2));
    }

    @Test
    void testMonthlyIncomeStatsWithRefund() throws Exception {
        Long userId = createTestWechatUser("退款统计测试用户");
        // 使用未来月份避免历史数据干扰
        YearMonth futureMonth = YearMonth.of(2030, 7);
        Long orderId = createPaidOrder(userId, "FAMILY_CARD", 50000L, futureMonth.atDay(1).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        createRefund(orderId, 10000L, futureMonth.atDay(15).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", futureMonth.toString())
                .param("end_month", futureMonth.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalIncomeCents").value(50000))
            .andExpect(jsonPath("$.data.totalRefundCents").value(10000))
            .andExpect(jsonPath("$.data.netIncomeCents").value(40000));
    }

    @Test
    void testMonthlyIncomeStatsWithMultipleOrders() throws Exception {
        Long userId = createTestWechatUser("多订单统计测试用户");
        // 使用未来月份避免历史数据干扰
        YearMonth futureMonth = YearMonth.of(2030, 6);

        createPaidOrder(userId, "FAMILY_CARD", 10000L, futureMonth.atDay(5).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        createPaidOrder(userId, "FAMILY_CARD", 20000L, futureMonth.atDay(10).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        createPaidOrder(userId, "VALUE_ADDED_SERVICE", 15000L, futureMonth.atDay(15).atStartOfDay(ZoneOffset.ofHours(8)).toOffsetDateTime());
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", futureMonth.toString())
                .param("end_month", futureMonth.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalIncomeCents").value(45000))
            .andExpect(jsonPath("$.data.netIncomeCents").value(45000));
    }

    @Test
    void testMonthlyIncomeStatsValidationError() throws Exception {
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", "invalid-month")
                .param("end_month", "2026-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testMonthlyIncomeStatsWithEmptyResult() throws Exception {
        sleepForRateLimit();

        mockMvc.perform(get("/api/admin/orders/monthly-income-stats")
                .header("Authorization", getAuthorizationHeader())
                .param("start_month", "2020-01")
                .param("end_month", "2020-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalIncomeCents").value(0))
            .andExpect(jsonPath("$.data.totalRefundCents").value(0))
            .andExpect(jsonPath("$.data.netIncomeCents").value(0));
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

    private Long createPaidOrder(Long buyerUserId, String orderType, Long amountCents, OffsetDateTime paidAt) {
        String orderNo = "O" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        OrderMainDO order = new OrderMainDO();
        order.setOrderNo(orderNo);
        order.setBuyerUserId(buyerUserId);
        order.setOrderType(orderType);
        order.setOrderStatus("PAID");
        order.setTotalAmountCents(amountCents);
        order.setDiscountAmountCents(0L);
        order.setPayableAmountCents(amountCents);
        order.setCurrency("CNY");
        order.setSourceChannel("WECHAT_MINI");
        order.setExpireAt(paidAt.plusHours(24));
        order.setPaidAt(paidAt);
        order.setCreatedAt(paidAt);
        order.setUpdatedAt(paidAt);
        orderMainMapper.insert(order);
        return order.getId();
    }

    private void createRefund(Long orderId, Long refundAmountCents, OffsetDateTime successTime) {
        // 先创建支付记录，然后创建退款记录
        PaymentTransactionDO payment = new PaymentTransactionDO();
        payment.setPaymentNo("PAY" + System.currentTimeMillis());
        payment.setOrderId(orderId);
        payment.setPaymentStatus("SUCCESS");
        payment.setChannel("WECHAT_PAY");
        payment.setOutTradeNo("OUT" + System.currentTimeMillis());
        payment.setTotalAmountCents(refundAmountCents);
        payment.setSuccessTime(successTime);
        payment.setCreatedAt(successTime);
        payment.setUpdatedAt(successTime);
        paymentTransactionMapper.insert(payment);

        PaymentRefundDO refund = new PaymentRefundDO();
        refund.setPaymentId(payment.getId());
        refund.setRefundNo("R" + System.currentTimeMillis());
        refund.setRefundAmountCents(refundAmountCents);
        refund.setReason("测试退款");
        refund.setRefundStatus("SUCCESS");
        refund.setWechatRefundId("wx_refund_" + System.currentTimeMillis());
        refund.setSuccessTime(successTime);
        refund.setCreatedAt(successTime);
        refund.setUpdatedAt(successTime);
        paymentRefundMapper.insert(refund);
    }
}
