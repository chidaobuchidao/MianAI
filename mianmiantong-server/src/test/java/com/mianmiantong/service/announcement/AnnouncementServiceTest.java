package com.mianmiantong.service.announcement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.entity.Announcement;
import com.mianmiantong.mapper.AnnouncementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementService")
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(announcementMapper);
    }

    private static Announcement announcement(Long id, int published) {
        Announcement a = new Announcement();
        a.setId(id);
        a.setIsPublished(published);
        return a;
    }

    @Nested
    @DisplayName("findLatestPublished")
    class FindLatestPublished {

        @Test
        @DisplayName("没有已发布公告时返回空，而不是 null")
        void returnsEmptyWhenNothingPublished() {
            when(announcementMapper.selectOne(any())).thenReturn(null);

            assertThat(service.findLatestPublished()).isEmpty();
        }

        @Test
        @DisplayName("返回 mapper 查到的公告")
        void returnsFoundAnnouncement() {
            Announcement latest = announcement(7L, 1);
            when(announcementMapper.selectOne(any())).thenReturn(latest);

            assertThat(service.findLatestPublished()).contains(latest);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("新建公告默认已发布并记录创建人")
        void publishesAndStampsCreator() {
            Announcement draft = new Announcement();
            when(announcementMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            Announcement created = service.create(draft, 42L);

            assertThat(created.getIsPublished()).isEqualTo(1);
            assertThat(created.getCreatedBy()).isEqualTo(42L);
            verify(announcementMapper).insert(draft);
        }

        @Test
        @DisplayName("未超过保留上限时不删除任何公告")
        void keepsAllWithinRetentionLimit() {
            when(announcementMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(existing(AnnouncementService.MAX_RETAINED));

            service.create(new Announcement(), 1L);

            verify(announcementMapper, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("超出保留上限的旧公告按倒序淘汰")
        void prunesOldestBeyondRetentionLimit() {
            when(announcementMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(existing(AnnouncementService.MAX_RETAINED + 2));

            service.create(new Announcement(), 1L);

            // listAll 按创建时间倒序，索引 MAX_RETAINED 起为待淘汰项
            verify(announcementMapper).deleteById(Long.valueOf(AnnouncementService.MAX_RETAINED));
            verify(announcementMapper).deleteById(Long.valueOf(AnnouncementService.MAX_RETAINED + 1));
        }

        private List<Announcement> existing(int count) {
            List<Announcement> all = new ArrayList<>();
            IntStream.range(0, count).forEach(i -> all.add(announcement((long) i, 1)));
            return all;
        }
    }

    @Nested
    @DisplayName("togglePublished")
    class TogglePublished {

        @Test
        @DisplayName("已发布的公告切换为下线")
        void publishedBecomesUnpublished() {
            when(announcementMapper.selectById(3L)).thenReturn(announcement(3L, 1));

            assertThat(service.togglePublished(3L)).contains(false);
            verify(announcementMapper).updateById(any(Announcement.class));
        }

        @Test
        @DisplayName("已下线的公告切换为发布")
        void unpublishedBecomesPublished() {
            when(announcementMapper.selectById(3L)).thenReturn(announcement(3L, 0));

            assertThat(service.togglePublished(3L)).contains(true);
        }

        @Test
        @DisplayName("isPublished 为 null 时视为未发布，切换后发布")
        void nullPublishedIsTreatedAsUnpublished() {
            Announcement withNullFlag = new Announcement();
            withNullFlag.setId(9L);
            when(announcementMapper.selectById(9L)).thenReturn(withNullFlag);

            assertThat(service.togglePublished(9L)).contains(true);
        }

        @Test
        @DisplayName("公告不存在时返回空，与「已下线」区分开")
        void missingAnnouncementIsDistinguishable() {
            when(announcementMapper.selectById(404L)).thenReturn(null);

            Optional<Boolean> result = service.togglePublished(404L);

            assertThat(result).isEmpty();
            verify(announcementMapper, never()).updateById(any(Announcement.class));
        }
    }
}
