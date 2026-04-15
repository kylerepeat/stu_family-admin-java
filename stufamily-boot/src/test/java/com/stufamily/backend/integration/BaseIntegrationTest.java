package com.stufamily.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.boot.StuFamilyBackendApplication;
import com.stufamily.backend.identity.infrastructure.persistence.dataobject.SysAdminUserDO;
import com.stufamily.backend.identity.infrastructure.persistence.mapper.SysAdminUserMapper;
import com.stufamily.backend.shared.security.JwtTokenProvider;
import com.stufamily.backend.shared.security.AuthAudience;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTest(classes = StuFamilyBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SysAdminUserMapper sysAdminUserMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    protected static final String TEST_ADMIN_USERNAME = "test_admin";
    protected static final String TEST_ADMIN_PASSWORD = "Test@123456";
    protected String adminToken;
    protected Long adminUserId;

    @BeforeEach
    void setUpBase() {
        adminUserId = createOrGetAdminUser();
        adminToken = generateAdminToken(adminUserId, TEST_ADMIN_USERNAME);
    }

    protected Long createOrGetAdminUser() {
        SysAdminUserDO existing = sysAdminUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysAdminUserDO>()
                .eq(SysAdminUserDO::getUsername, TEST_ADMIN_USERNAME)
        );
        if (existing != null) {
            return existing.getId();
        }

        SysAdminUserDO user = new SysAdminUserDO();
        user.setUserNo("A" + System.currentTimeMillis());
        user.setUsername(TEST_ADMIN_USERNAME);
        user.setPasswordHash(passwordEncoder.encode(TEST_ADMIN_PASSWORD));
        user.setStatus("ACTIVE");
        user.setTokenVersion(1L);
        user.setNickname("Test Admin");
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        sysAdminUserMapper.insert(user);
        return user.getId();
    }

    protected String generateAdminToken(Long userId, String username) {
        return jwtTokenProvider.createAccessToken(
            userId,
            username,
            List.of("ADMIN"),
            AuthAudience.ADMIN,
            1L
        );
    }

    protected String getAuthorizationHeader() {
        return "Bearer " + adminToken;
    }

    protected void sleepForRateLimit() throws InterruptedException {
        Thread.sleep(3100);
    }
}
