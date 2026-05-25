package com.mianmiantong.controller.admin;

import com.mianmiantong.common.Result;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.entity.Announcement;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.mapper.AnnouncementMapper;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.mapper.user.UserMapper;
import com.mianmiantong.mapper.user.UserAiConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final UserAiConfigMapper aiConfigMapper;
    private final InterviewSessionMapper sessionMapper;
    private final AnnouncementMapper announcementMapper;

    @Value("${DEEPSEEK_API_KEY:}")
    private String systemApiKey;

    public AdminController(UserMapper userMapper, UserAiConfigMapper aiConfigMapper,
                           InterviewSessionMapper sessionMapper,
                           AnnouncementMapper announcementMapper) {
        this.userMapper = userMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.sessionMapper = sessionMapper;
        this.announcementMapper = announcementMapper;
    }

    /** All admin endpoints require role=1 */
    private void requireAdmin() {
        if (!JwtAuthFilter.isAdmin()) {
            throw new IllegalArgumentException("无管理员权限");
        }
    }

    /** System status overview */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        requireAdmin();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalSessions", sessionMapper.selectCount(null));
        data.put("hasSystemKey", systemApiKey != null && !systemApiKey.isBlank());
        // Count users with API key configured
        data.put("usersWithKey", aiConfigMapper.selectCount(null));
        return Result.ok(data);
    }

    /** User list with pagination + search */
    @GetMapping("/users")
    public Result<Map<String, Object>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        requireAdmin();
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(User::getNickname, keyword).or().like(User::getUsername, keyword));
        }
        qw.orderByDesc(User::getId);
        long total = userMapper.selectCount(qw);
        qw.last("LIMIT " + ((page - 1) * pageSize) + "," + pageSize);
        List<User> users = userMapper.selectList(qw);

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("nickname", u.getNickname());
            row.put("role", u.getRole() != null && u.getRole() == 1 ? "管理员" : "普通用户");
            row.put("hasApiKey", aiConfigMapper.selectById(u.getId()) != null);
            row.put("dailyQuota", u.getDailyQuota());
            row.put("quotaUsed", u.getQuotaUsed());
            row.put("createTime", u.getCreateTime());
            Long count = sessionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                    com.mianmiantong.entity.interview.InterviewSession>()
                    .eq(com.mianmiantong.entity.interview.InterviewSession::getUserId, u.getId())
            );
            row.put("interviewCount", count);
            list.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    /** Interview sessions with pagination + search */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> sessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        requireAdmin();
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
            com.mianmiantong.entity.interview.InterviewSession>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(com.mianmiantong.entity.interview.InterviewSession::getPosition, keyword);
        }
        qw.orderByDesc(com.mianmiantong.entity.interview.InterviewSession::getId);
        long total = sessionMapper.selectCount(qw);
        qw.last("LIMIT " + ((page - 1) * pageSize) + "," + pageSize);
        var sessions = sessionMapper.selectList(qw);

        List<Map<String, Object>> list = new ArrayList<>();
        for (var s : sessions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("userId", s.getUserId());
            row.put("position", s.getPosition());
            row.put("score", s.getOverallScore());
            row.put("status", s.getStatus() == 1 ? "已结束" : "进行中");
            row.put("createTime", s.getCreateTime());
            User u = userMapper.selectById(s.getUserId());
            row.put("userName", u != null ? u.getNickname() : "未知");
            list.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    /** Set user remaining free quota (admin sets remaining, we compute quotaUsed) */
    @PostMapping("/set-quota")
    public Result<?> setQuota(@RequestBody Map<String, Object> body) {
        requireAdmin();
        Long userId = Long.valueOf(body.get("userId").toString());
        int remaining = Integer.parseInt(body.get("remaining").toString());
        var user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        int daily = user.getDailyQuota() != null ? user.getDailyQuota() : 10;
        user.setQuotaUsed(Math.max(0, daily - remaining));
        userMapper.updateById(user);
        return Result.ok(Map.of("message", "已更新"));
    }

    /** Set daily quota limit */
    @PostMapping("/set-limit")
    public Result<?> setLimit(@RequestBody Map<String, Object> body) {
        requireAdmin();
        Long userId = Long.valueOf(body.get("userId").toString());
        int limit = Integer.parseInt(body.get("limit").toString());
        var user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        user.setDailyQuota(limit);
        userMapper.updateById(user);
        return Result.ok(Map.of("message", "已更新"));
    }

    /** Toggle user admin role */
    @PostMapping("/toggle-admin")
    public Result<?> toggleAdmin(@RequestBody Map<String, Object> body) {
        requireAdmin();
        Long userId = Long.valueOf(body.get("userId").toString());
        var user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        boolean makeAdmin = user.getRole() == null || user.getRole() != 1;
        user.setRole(makeAdmin ? 1 : 0);
        userMapper.updateById(user);
        return Result.ok(Map.of("message", makeAdmin ? "已设为管理员" : "已取消管理员"));
    }

    /** Delete user */
    @PostMapping("/delete-user")
    public Result<?> deleteUser(@RequestBody Map<String, Object> body) {
        requireAdmin();
        Long userId = Long.valueOf(body.get("userId").toString());
        if (userId == 1L || userId == getUser().getId()) {
            throw new IllegalArgumentException("不能删除自己的账号");
        }
        userMapper.deleteById(userId);
        return Result.ok(Map.of("message", "已删除"));
    }

    private User getUser() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        return userId != null ? userMapper.selectById(userId) : null;
    }

    /** Clear all test interview sessions */
    @PostMapping("/clear-sessions")
    public Result<?> clearSessions() {
        requireAdmin();
        sessionMapper.delete(null); // truncate-all
        return Result.ok(Map.of("message", "已清空所有面试记录"));
    }

    /** Clear ALL test data */
    @PostMapping("/clear-all")
    public Result<?> clearAll() {
        requireAdmin();
        sessionMapper.delete(null);
        return Result.ok(Map.of("message", "已清空面试记录"));
    }

    // ==================== 公告管理 ====================

    @GetMapping("/announcements")
    public Result<List<Announcement>> listAnnouncements() {
        requireAdmin();
        return Result.ok(announcementMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreateTime)));
    }

    @PostMapping("/announcement")
    public Result<Announcement> createAnnouncement(@RequestBody Announcement a) {
        requireAdmin();
        a.setCreatedBy(getUserId());
        a.setIsPublished(1);
        announcementMapper.insert(a);
        cleanupAnnouncements();
        return Result.ok(a);
    }

    @PutMapping("/announcement/{id}")
    public Result<?> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement a) {
        requireAdmin();
        a.setId(id);
        a.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(a);
        return Result.ok();
    }

    @DeleteMapping("/announcement/{id}")
    public Result<?> deleteAnnouncement(@PathVariable Long id) {
        requireAdmin();
        announcementMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/announcement/{id}/publish")
    public Result<?> togglePublish(@PathVariable Long id) {
        requireAdmin();
        Announcement a = announcementMapper.selectById(id);
        if (a != null) {
            a.setIsPublished(a.getIsPublished() == 1 ? 0 : 1);
            announcementMapper.updateById(a);
        }
        return Result.ok(Map.of("published", a != null && a.getIsPublished() == 1));
    }

    private void cleanupAnnouncements() {
        List<Announcement> all = announcementMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreateTime));
        if (all.size() > 5) {
            for (int i = 5; i < all.size(); i++) {
                announcementMapper.deleteById(all.get(i).getId());
            }
        }
    }

    private Long getUserId() {
        return JwtAuthFilter.getCurrentUserId();
    }
}
