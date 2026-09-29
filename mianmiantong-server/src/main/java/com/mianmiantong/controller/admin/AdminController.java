package com.mianmiantong.controller.admin;

import com.mianmiantong.common.ForbiddenException;
import com.mianmiantong.common.Result;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.admin.AdminSessionRow;
import com.mianmiantong.dto.admin.AdminStatus;
import com.mianmiantong.dto.admin.AdminUserRow;
import com.mianmiantong.dto.admin.PageResponse;
import com.mianmiantong.dto.admin.SetLimitRequest;
import com.mianmiantong.dto.admin.SetQuotaRequest;
import com.mianmiantong.dto.admin.TargetUserRequest;
import com.mianmiantong.entity.Announcement;
import com.mianmiantong.service.admin.AdminService;
import com.mianmiantong.service.announcement.AnnouncementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AnnouncementService announcementService;

    public AdminController(AdminService adminService, AnnouncementService announcementService) {
        this.adminService = adminService;
        this.announcementService = announcementService;
    }

    /** All admin endpoints require role=1 */
    private void requireAdmin() {
        if (!JwtAuthFilter.isAdmin()) {
            throw new ForbiddenException("无管理员权限");
        }
    }

    /** System status overview */
    @GetMapping("/status")
    public Result<AdminStatus> status() {
        requireAdmin();
        return Result.ok(adminService.status());
    }

    /** User list with pagination + search */
    @GetMapping("/users")
    public Result<PageResponse<AdminUserRow>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        requireAdmin();
        return Result.ok(adminService.listUsers(page, pageSize, keyword));
    }

    /** Interview sessions with pagination + search */
    @GetMapping("/sessions")
    public Result<PageResponse<AdminSessionRow>> sessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        requireAdmin();
        return Result.ok(adminService.listSessions(page, pageSize, keyword));
    }

    /** Set user remaining free quota (admin sets remaining, we compute quotaUsed) */
    @PostMapping("/set-quota")
    public Result<?> setQuota(@Valid @RequestBody SetQuotaRequest request) {
        requireAdmin();
        adminService.setRemainingQuota(request.userId(), request.remaining());
        return Result.ok(Map.of("message", "已更新"));
    }

    /** Set daily quota limit */
    @PostMapping("/set-limit")
    public Result<?> setLimit(@Valid @RequestBody SetLimitRequest request) {
        requireAdmin();
        adminService.setDailyLimit(request.userId(), request.limit());
        return Result.ok(Map.of("message", "已更新"));
    }

    /** Toggle user admin role */
    @PostMapping("/toggle-admin")
    public Result<?> toggleAdmin(@Valid @RequestBody TargetUserRequest request) {
        requireAdmin();
        boolean nowAdmin = adminService.toggleAdmin(request.userId());
        return Result.ok(Map.of("message", nowAdmin ? "已设为管理员" : "已取消管理员"));
    }

    /** Toggle paper knowledge base access for users without their own API key */
    @PostMapping("/toggle-knowledge-base")
    public Result<?> toggleKnowledgeBase(@Valid @RequestBody TargetUserRequest request) {
        requireAdmin();
        boolean enabled = adminService.toggleKnowledgeBase(request.userId());
        return Result.ok(Map.of("enabled", enabled, "message", enabled ? "已开放知识库" : "已关闭知识库"));
    }

    /** Delete user */
    @PostMapping("/delete-user")
    public Result<?> deleteUser(@Valid @RequestBody TargetUserRequest request) {
        requireAdmin();
        adminService.deleteUser(request.userId(), JwtAuthFilter.getCurrentUserId());
        return Result.ok(Map.of("message", "已删除"));
    }

    /** Clear all test interview sessions */
    @PostMapping("/clear-sessions")
    public Result<?> clearSessions() {
        requireAdmin();
        adminService.clearAllSessions();
        return Result.ok(Map.of("message", "已清空所有面试记录"));
    }

    /**
     * Legacy alias of /clear-sessions: despite the name it only clears interview sessions.
     * Neither frontend calls it.
     */
    @PostMapping("/clear-all")
    public Result<?> clearAll() {
        requireAdmin();
        adminService.clearAllSessions();
        return Result.ok(Map.of("message", "已清空面试记录"));
    }

    // ==================== 公告管理 ====================

    @GetMapping("/announcements")
    public Result<List<Announcement>> listAnnouncements() {
        requireAdmin();
        return Result.ok(announcementService.listAll());
    }

    @PostMapping("/announcement")
    public Result<Announcement> createAnnouncement(@RequestBody Announcement a) {
        requireAdmin();
        return Result.ok(announcementService.create(a, getUserId()));
    }

    @PutMapping("/announcement/{id}")
    public Result<?> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement a) {
        requireAdmin();
        announcementService.update(id, a);
        return Result.ok();
    }

    @DeleteMapping("/announcement/{id}")
    public Result<?> deleteAnnouncement(@PathVariable Long id) {
        requireAdmin();
        announcementService.delete(id);
        return Result.ok();
    }

    @PostMapping("/announcement/{id}/publish")
    public Result<?> togglePublish(@PathVariable Long id) {
        requireAdmin();
        return announcementService.togglePublished(id)
            .<Result<?>>map(published -> Result.ok(Map.of("published", published)))
            .orElseGet(() -> Result.fail("公告不存在"));
    }

    private Long getUserId() {
        return JwtAuthFilter.getCurrentUserId();
    }
}
