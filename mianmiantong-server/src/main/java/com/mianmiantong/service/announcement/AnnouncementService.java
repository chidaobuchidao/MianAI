package com.mianmiantong.service.announcement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.entity.Announcement;
import com.mianmiantong.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 公告业务逻辑。
 *
 * <p>集中承载公告的读写规则，供用户端（只读最新一条）与管理端（增删改与发布切换）共用，
 * 避免两个 controller 各自持有 mapper 并复制保留条数之类的规则。
 */
@Service
public class AnnouncementService {

    /** 最多保留的公告条数，超出的按创建时间倒序淘汰。 */
    static final int MAX_RETAINED = 5;

    private final AnnouncementMapper announcementMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    /** 最新一条已发布公告；没有则为空。 */
    public Optional<Announcement> findLatestPublished() {
        return Optional.ofNullable(announcementMapper.selectOne(
            new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getIsPublished, 1)
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT 1")
        ));
    }

    /** 全部公告，按创建时间倒序。 */
    public List<Announcement> listAll() {
        return announcementMapper.selectList(
            new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreateTime)
        );
    }

    /** 新建公告并立即发布，同时淘汰超出保留上限的旧公告。 */
    @Transactional
    public Announcement create(Announcement announcement, Long createdBy) {
        announcement.setCreatedBy(createdBy);
        announcement.setIsPublished(1);
        announcementMapper.insert(announcement);
        pruneBeyondRetentionLimit();
        return announcement;
    }

    /** 按 id 更新公告内容。 */
    public void update(Long id, Announcement announcement) {
        announcement.setId(id);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
    }

    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    /**
     * 切换发布状态。
     *
     * @return 切换后的发布状态；公告不存在时为空，便于调用方区分「不存在」与「已下线」。
     */
    public Optional<Boolean> togglePublished(Long id) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            return Optional.empty();
        }
        existing.setIsPublished(isPublished(existing) ? 0 : 1);
        announcementMapper.updateById(existing);
        return Optional.of(isPublished(existing));
    }

    /** 发布状态判定；字段可空，null 视为未发布。 */
    private static boolean isPublished(Announcement announcement) {
        return Integer.valueOf(1).equals(announcement.getIsPublished());
    }

    private void pruneBeyondRetentionLimit() {
        List<Announcement> all = listAll();
        for (int i = MAX_RETAINED; i < all.size(); i++) {
            announcementMapper.deleteById(all.get(i).getId());
        }
    }
}
