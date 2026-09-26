package com.mianmiantong.controller;

import com.mianmiantong.common.Result;
import com.mianmiantong.entity.Announcement;
import com.mianmiantong.service.announcement.AnnouncementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** 获取最新一条已发布公告 */
    @GetMapping("/latest")
    public Result<Announcement> latest() {
        return Result.ok(announcementService.findLatestPublished().orElse(null));
    }
}
