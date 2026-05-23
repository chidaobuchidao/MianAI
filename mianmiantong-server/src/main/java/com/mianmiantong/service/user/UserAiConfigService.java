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

    public boolean hasApiKey(Long userId) {
        if (userId == null) return false;
        UserAiConfig config = mapper.selectById(userId);
        return config != null && config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    public void save(Long userId, String provider, String apiKey, String model) {
        // apiKey为空时删除记录，回退使用系统默认Key
        if (apiKey == null || apiKey.isBlank()) {
            mapper.deleteById(userId);
            return;
        }

        UserAiConfig existing = mapper.selectById(userId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setProvider(provider);
            existing.setApiKey(apiKey);
            existing.setModel(model);
            existing.setUpdateTime(now);
            mapper.updateById(existing);
        } else {
            UserAiConfig config = new UserAiConfig();
            config.setUserId(userId);
            config.setProvider(provider);
            config.setApiKey(apiKey);
            config.setModel(model);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            mapper.insert(config);
        }
    }
}
