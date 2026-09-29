package com.mianmiantong.controller.admin;

import com.mianmiantong.common.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin API contract against the real security filter, controller and H2 database.
 *
 * <p>Fixtures use random ids and a per-test tag that doubles as the search keyword, so listings
 * only ever see rows created by the current test even though the H2 database is shared with the
 * other Spring context tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtUtil jwt;

    @MockBean private StringRedisTemplate stringRedisTemplate;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> sessionIds = new ArrayList<>();
    private String tag;
    private long adminId;
    private long aliceId;
    private long bobId;
    private long missingUserId;
    private LocalDate today;

    @BeforeEach
    void insertFixtures() {
        tag = "admin-it-" + UUID.randomUUID().toString().substring(0, 8);
        today = LocalDate.now();
        long base = ThreadLocalRandom.current().nextLong(2_000_000_000L, 3_000_000_000L);
        adminId = base;
        aliceId = base + 1;
        bobId = base + 2;
        missingUserId = base + 99;

        insertUser(adminId, "admin", 1, 10, 0, today, 0);
        insertUser(aliceId, "alice", 0, 10, 3, today, 1);
        insertUser(bobId, "bob", 0, null, null, null, 0);
        jdbc.update("INSERT INTO user_ai_config (user_id, provider, api_key) VALUES (?, ?, ?)",
                aliceId, "deepseek", "sk-fixture");

        insertSession(base + 10, aliceId, "java", 88, 1);
        insertSession(base + 11, aliceId, "go", null, 0);
        insertSession(base + 12, missingUserId, "orphan", 70, 1);
    }

    @AfterEach
    void removeOnlyOwnFixtures() {
        for (Long id : userIds) {
            jdbc.update("DELETE FROM user_ai_config WHERE user_id = ?", id);
            jdbc.update("DELETE FROM sys_user WHERE id = ?", id);
        }
        for (Long id : sessionIds) {
            jdbc.update("DELETE FROM interview_session WHERE id = ?", id);
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void statusCountsUsersSessionsAndConfiguredKeys() throws Exception {
        long users = count("sys_user");
        long sessions = count("interview_session");
        long configs = count("user_ai_config");

        mvc.perform(get("/api/admin/status").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(users))
                .andExpect(jsonPath("$.data.totalSessions").value(sessions))
                .andExpect(jsonPath("$.data.usersWithKey").value(configs))
                .andExpect(jsonPath("$.data.hasSystemKey").isBoolean());
    }

    @Test
    void userListingIsNewestFirstWithPerUserFacts() throws Exception {
        mvc.perform(get("/api/admin/users").param("keyword", tag).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.list.length()").value(3))
                .andExpect(jsonPath("$.data.list[0].id").value(bobId))
                .andExpect(jsonPath("$.data.list[0].role").value("普通用户"))
                .andExpect(jsonPath("$.data.list[0].hasApiKey").value(false))
                .andExpect(jsonPath("$.data.list[0].knowledgeBaseEnabled").value(false))
                .andExpect(jsonPath("$.data.list[0].interviewCount").value(0))
                .andExpect(jsonPath("$.data.list[1].id").value(aliceId))
                .andExpect(jsonPath("$.data.list[1].nickname").value(tag + "-alice"))
                .andExpect(jsonPath("$.data.list[1].hasApiKey").value(true))
                .andExpect(jsonPath("$.data.list[1].knowledgeBaseEnabled").value(true))
                .andExpect(jsonPath("$.data.list[1].dailyQuota").value(10))
                .andExpect(jsonPath("$.data.list[1].quotaUsed").value(3))
                .andExpect(jsonPath("$.data.list[1].interviewCount").value(2))
                .andExpect(jsonPath("$.data.list[1].createTime").isNotEmpty())
                .andExpect(jsonPath("$.data.list[2].id").value(adminId))
                .andExpect(jsonPath("$.data.list[2].role").value("管理员"));
    }

    @Test
    void userListingResetsStaleDailyUsage() throws Exception {
        mvc.perform(get("/api/admin/users").param("keyword", tag + "-bob").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].quotaUsed").value(0));

        assertThat(jdbc.queryForObject("SELECT quota_date FROM sys_user WHERE id = ?", LocalDate.class, bobId))
                .isEqualTo(today);
    }

    @Test
    void userListingPagesByOffset() throws Exception {
        mvc.perform(get("/api/admin/users").param("keyword", tag).param("page", "2").param("pageSize", "2")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(adminId));
    }

    @Test
    void pageBeyondTheLastReturnsAnEmptyList() throws Exception {
        mvc.perform(get("/api/admin/users").param("keyword", tag).param("page", "9")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    void sessionListingNamesUsersAndReportsStatus() throws Exception {
        mvc.perform(get("/api/admin/sessions").param("keyword", tag).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].position").value(tag + "-orphan"))
                .andExpect(jsonPath("$.data.list[0].userName").value("未知"))
                .andExpect(jsonPath("$.data.list[1].position").value(tag + "-go"))
                .andExpect(jsonPath("$.data.list[1].status").value("进行中"))
                .andExpect(jsonPath("$.data.list[1].userName").value(tag + "-alice"))
                .andExpect(jsonPath("$.data.list[2].position").value(tag + "-java"))
                .andExpect(jsonPath("$.data.list[2].status").value("已结束"))
                .andExpect(jsonPath("$.data.list[2].score").value(88))
                .andExpect(jsonPath("$.data.list[2].userId").value(aliceId));
    }

    @Test
    void setQuotaStoresUsageAsLimitMinusRemaining() throws Exception {
        postJson("/api/admin/set-quota", "{\"userId\":" + aliceId + ",\"remaining\":4}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("已更新"));

        assertThat(intColumn("quota_used", aliceId)).isEqualTo(6);
    }

    @Test
    void setQuotaFallsBackToTheDefaultLimitWhenUnset() throws Exception {
        postJson("/api/admin/set-quota", "{\"userId\":" + bobId + ",\"remaining\":7}")
                .andExpect(status().isOk());

        assertThat(intColumn("quota_used", bobId)).isEqualTo(3);
    }

    @Test
    void setLimitStoresTheDailyQuota() throws Exception {
        postJson("/api/admin/set-limit", "{\"userId\":" + aliceId + ",\"limit\":25}")
                .andExpect(status().isOk());

        assertThat(intColumn("daily_quota", aliceId)).isEqualTo(25);
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        postJson("/api/admin/set-limit", "{\"userId\":" + missingUserId + ",\"limit\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void invalidRequestBodiesAreRejectedAndChangeNothing() throws Exception {
        postJson("/api/admin/set-limit", "{\"limit\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId不能为空"));
        postJson("/api/admin/set-limit", "{\"userId\":" + aliceId + ",\"limit\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit不能为负数"));
        postJson("/api/admin/set-quota", "{\"userId\":" + aliceId + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("remaining不能为空"));
        postJson("/api/admin/toggle-admin", "{\"userId\":\"abc\"}")
                .andExpect(status().isBadRequest());
        postJson("/api/admin/delete-user", "")
                .andExpect(status().isBadRequest());

        assertThat(intColumn("daily_quota", aliceId)).isEqualTo(10);
        assertThat(intColumn("quota_used", aliceId)).isEqualTo(3);
        assertThat(intColumn("role", aliceId)).isZero();
    }

    @Test
    void toggleAdminFlipsTheRole() throws Exception {
        postJson("/api/admin/toggle-admin", "{\"userId\":" + bobId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("已设为管理员"));
        assertThat(intColumn("role", bobId)).isEqualTo(1);

        postJson("/api/admin/toggle-admin", "{\"userId\":" + bobId + "}")
                .andExpect(jsonPath("$.data.message").value("已取消管理员"));
        assertThat(intColumn("role", bobId)).isZero();
    }

    @Test
    void toggleKnowledgeBaseFlipsAccess() throws Exception {
        postJson("/api/admin/toggle-knowledge-base", "{\"userId\":" + bobId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        postJson("/api/admin/toggle-knowledge-base", "{\"userId\":" + bobId + "}")
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void deleteUserRemovesTheAccount() throws Exception {
        postJson("/api/admin/delete-user", "{\"userId\":" + bobId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("已删除"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id = ?", Long.class, bobId)).isZero();
    }

    @Test
    void administratorsCannotDeleteThemselves() throws Exception {
        postJson("/api/admin/delete-user", "{\"userId\":" + adminId + "}")
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id = ?", Long.class, adminId))
                .isEqualTo(1L);
    }

    @Test
    void theInitialAdministratorCannotBeDeleted() throws Exception {
        postJson("/api/admin/delete-user", "{\"userId\":1}")
                .andExpect(status().isBadRequest());
    }

    /** Wipes the whole interview_session table by design; no other test keeps sessions across tests. */
    @Test
    void clearSessionsRemovesEverySession() throws Exception {
        mvc.perform(post("/api/admin/clear-sessions").header("Authorization", adminToken()))
                .andExpect(status().isOk());

        assertThat(count("interview_session")).isZero();
    }

    @Test
    void nonAdministratorsAreForbidden() throws Exception {
        mvc.perform(get("/api/admin/status").header("Authorization", bearer(aliceId, 0)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无管理员权限"));

        postJsonAs(bearer(aliceId, 0), "/api/admin/toggle-admin", "{\"userId\":" + bobId + "}")
                .andExpect(status().isForbidden());
        assertThat(intColumn("role", bobId)).isZero();
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return postJsonAs(adminToken(), path, body);
    }

    private ResultActions postJsonAs(String token, String path, String body) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        return mvc.perform(request);
    }

    private String adminToken() {
        return bearer(adminId, 1);
    }

    private String bearer(long userId, int role) {
        return "Bearer " + jwt.generateToken(userId, tag + "-openid-" + userId, role);
    }

    private void insertUser(long id, String name, int role, Integer dailyQuota, Integer quotaUsed,
                            LocalDate quotaDate, int knowledgeBase) {
        jdbc.update("INSERT INTO sys_user (id, openid, nickname, username, role, daily_quota, quota_used, "
                        + "quota_date, knowledge_base_enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tag + "-openid-" + id, tag + "-" + name, tag + "-" + name, role,
                dailyQuota, quotaUsed, quotaDate, knowledgeBase);
        userIds.add(id);
    }

    private void insertSession(long id, long userId, String position, Integer score, int sessionStatus) {
        jdbc.update("INSERT INTO interview_session (id, user_id, position, overall_score, status) VALUES (?, ?, ?, ?, ?)",
                id, userId, tag + "-" + position, score, sessionStatus);
        sessionIds.add(id);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private Integer intColumn(String column, long userId) {
        return jdbc.queryForObject("SELECT " + column + " FROM sys_user WHERE id = ?", Integer.class, userId);
    }
}
