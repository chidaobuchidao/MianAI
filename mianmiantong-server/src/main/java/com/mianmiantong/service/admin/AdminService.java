package com.mianmiantong.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.dto.admin.AdminSessionRow;
import com.mianmiantong.dto.admin.AdminStatus;
import com.mianmiantong.dto.admin.AdminUserRow;
import com.mianmiantong.dto.admin.PageResponse;
import com.mianmiantong.entity.interview.InterviewSession;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.user.UserAiConfigMapper;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.service.user.QuotaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台的用户与面试会话管理。
 *
 * <p>管理员校验留在 controller，这里只承载数据读写与业务规则，调用方需先确认操作者是管理员。
 */
@Service
public class AdminService {

    /** 初始管理员账号，任何人都不能删除。 */
    private static final long INITIAL_ADMIN_ID = 1L;

    /** 单页上限，与 {@code PageQuery} 一致。用户列表每行还要补查最多三次，页越大放大越多。 */
    private static final int MAX_PAGE_SIZE = 100;

    private static final int ROLE_USER = 0;
    private static final int ROLE_ADMIN = 1;
    private static final int FLAG_OFF = 0;
    private static final int FLAG_ON = 1;
    private static final int SESSION_FINISHED = 1;

    private final UserMapper userMapper;
    private final UserAiConfigMapper aiConfigMapper;
    private final InterviewSessionMapper sessionMapper;
    private final QuotaService quotaService;
    private final boolean systemKeyConfigured;

    public AdminService(UserMapper userMapper,
                        UserAiConfigMapper aiConfigMapper,
                        InterviewSessionMapper sessionMapper,
                        QuotaService quotaService,
                        @Value("${ai.deepseek.api-key:}") String systemApiKey) {
        this.userMapper = userMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.sessionMapper = sessionMapper;
        this.quotaService = quotaService;
        // 与 KeyResolver 读同一个属性，状态页反映的就是网关实际使用的系统 Key
        this.systemKeyConfigured = systemApiKey != null && !systemApiKey.isBlank();
    }

    public AdminStatus status() {
        return new AdminStatus(
            userMapper.selectCount(null),
            sessionMapper.selectCount(null),
            systemKeyConfigured,
            aiConfigMapper.selectCount(null));
    }

    /**
     * 用户列表，按 id 倒序分页，可按昵称或用户名模糊搜索。
     *
     * <p>逐行刷新过期的每日用量（会写库），与配额接口看到的已用次数保持一致。
     */
    public PageResponse<AdminUserRow> listUsers(int page, int pageSize, String keyword) {
        String limit = limitClause(page, pageSize);
        long total = userMapper.selectCount(userFilter(keyword));
        List<User> users = userMapper.selectList(
            userFilter(keyword).orderByDesc(User::getId).last(limit));

        List<AdminUserRow> rows = new ArrayList<>(users.size());
        for (User user : users) {
            quotaService.refreshDailyQuota(user);
            rows.add(toUserRow(user));
        }
        return new PageResponse<>(rows, total, page, pageSize);
    }

    /** 面试会话列表，按 id 倒序分页，可按岗位模糊搜索。 */
    public PageResponse<AdminSessionRow> listSessions(int page, int pageSize, String keyword) {
        String limit = limitClause(page, pageSize);
        long total = sessionMapper.selectCount(sessionFilter(keyword));
        List<InterviewSession> sessions = sessionMapper.selectList(
            sessionFilter(keyword).orderByDesc(InterviewSession::getId).last(limit));

        List<AdminSessionRow> rows = new ArrayList<>(sessions.size());
        for (InterviewSession session : sessions) {
            rows.add(toSessionRow(session, userMapper.selectById(session.getUserId())));
        }
        return new PageResponse<>(rows, total, page, pageSize);
    }

    /** 按「今日剩余次数」设置配额：已用次数 = 每日上限 - 剩余，不低于 0。 */
    public void setRemainingQuota(Long userId, int remaining) {
        User user = requireUser(userId);
        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : QuotaService.DEFAULT_DAILY_QUOTA;
        user.setQuotaUsed(Math.max(0, daily - remaining));
        user.setQuotaDate(LocalDate.now());
        userMapper.updateById(user);
    }

    /** 设置每日上限，写入前先按日期刷新已用次数。 */
    public void setDailyLimit(Long userId, int limit) {
        User user = requireUser(userId);
        quotaService.refreshDailyQuota(user);
        user.setDailyQuota(limit);
        userMapper.updateById(user);
    }

    /** @return 切换后是否为管理员 */
    public boolean toggleAdmin(Long userId) {
        User user = requireUser(userId);
        boolean makeAdmin = !Integer.valueOf(ROLE_ADMIN).equals(user.getRole());
        user.setRole(makeAdmin ? ROLE_ADMIN : ROLE_USER);
        userMapper.updateById(user);
        return makeAdmin;
    }

    /** @return 切换后是否开放论文知识库 */
    public boolean toggleKnowledgeBase(Long userId) {
        User user = requireUser(userId);
        boolean enable = !Integer.valueOf(FLAG_ON).equals(user.getKnowledgeBaseEnabled());
        user.setKnowledgeBaseEnabled(enable ? FLAG_ON : FLAG_OFF);
        userMapper.updateById(user);
        return enable;
    }

    /**
     * 删除用户账号。初始管理员与操作者本人不可删。
     *
     * <p>只删 sys_user 一行：该用户的面试会话、AI 配置等数据没有外键，不会级联删除。
     */
    public void deleteUser(Long userId, Long actingUserId) {
        if (userId == INITIAL_ADMIN_ID || userId.equals(actingUserId)) {
            throw new IllegalArgumentException("不能删除自己的账号");
        }
        userMapper.deleteById(userId);
    }

    /** 清空全部面试会话（整表删除）。 */
    public void clearAllSessions() {
        sessionMapper.delete(null);
    }

    private AdminUserRow toUserRow(User user) {
        boolean admin = Integer.valueOf(ROLE_ADMIN).equals(user.getRole());
        return new AdminUserRow(
            user.getId(),
            user.getNickname(),
            admin ? "管理员" : "普通用户",
            aiConfigMapper.selectById(user.getId()) != null,
            Integer.valueOf(FLAG_ON).equals(user.getKnowledgeBaseEnabled()),
            user.getDailyQuota(),
            user.getQuotaUsed(),
            user.getCreateTime(),
            sessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, user.getId())));
    }

    /** @param owner 会话所属用户；已被删除时为 null，显示为「未知」 */
    private static AdminSessionRow toSessionRow(InterviewSession session, User owner) {
        boolean finished = Integer.valueOf(SESSION_FINISHED).equals(session.getStatus());
        return new AdminSessionRow(
            session.getId(),
            session.getUserId(),
            session.getPosition(),
            session.getOverallScore(),
            finished ? "已结束" : "进行中",
            session.getCreateTime(),
            owner != null ? owner.getNickname() : "未知");
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    /** 校验分页参数后生成 LIMIT 子句。offset 按 long 计算，大页码不会溢出成负数。 */
    private static String limitClause(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("page必须大于0");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize必须在1到" + MAX_PAGE_SIZE + "之间");
        }
        long offset = (long) (page - 1) * pageSize;
        return "LIMIT " + offset + "," + pageSize;
    }

    /**
     * 只含筛选条件，计数查询因此不带分页查询的 ORDER BY。对 COUNT(*) 排序没有意义，
     * H2 还会把它当作聚合查询里的非聚合列直接拒绝。
     */
    private static LambdaQueryWrapper<User> userFilter(String keyword) {
        var qw = new LambdaQueryWrapper<User>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword));
        }
        return qw;
    }

    private static LambdaQueryWrapper<InterviewSession> sessionFilter(String keyword) {
        var qw = new LambdaQueryWrapper<InterviewSession>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(InterviewSession::getPosition, keyword);
        }
        return qw;
    }
}
