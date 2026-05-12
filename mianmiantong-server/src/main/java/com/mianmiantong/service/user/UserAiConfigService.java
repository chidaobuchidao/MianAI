package com.mianmiantong.service.user;

import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.user.UserAiConfigMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserAiConfigService {

    private final UserAiConfigMapper mapper;

    public UserAiConfigService(UserAiConfigMapper mapper) {
        this.mapper = mapper;
    }

    public UserAiConfig getByUserId(Long userId) {
        return mapper.selectById(userId);
    }

    public void save(Long userId, String provider, String apiKey) {
        UserAiConfig existing = mapper.selectById(userId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setProvider(provider);
            existing.setApiKey(apiKey);
            existing.setUpdateTime(now);
            mapper.updateById(existing);
        } else {
            UserAiConfig config = new UserAiConfig();
            config.setUserId(userId);
            config.setProvider(provider);
            config.setApiKey(apiKey);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            mapper.insert(config);
        }
    }
}
