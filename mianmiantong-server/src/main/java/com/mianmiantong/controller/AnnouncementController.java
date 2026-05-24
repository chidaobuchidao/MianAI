package com.mianmiantong.controller;

import com.mianmiantong.common.Result;
import com.mianmiantong.entity.Announcement;
import com.mianmiantong.mapper.AnnouncementMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementController(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    /** 获取最新一条已发布公告 */
    @GetMapping("/latest")
    public Result<Announcement> latest() {
        Announcement a = announcementMapper.selectOne(
            new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getIsPublished, 1)
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT 1")
        );
        return Result.ok(a);
    }
}
