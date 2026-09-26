package com.mianmiantong.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 系统预置的 AI 提供者列表。
 *
 * <p>`provider-presets.json` 是打包进 classpath 的静态资源，运行期不会变，
 * 因此在启动时读一次并缓存；此前每次请求都重新建 ObjectMapper 并重读文件。
 * 读取失败时降级为空列表，不影响应用启动。
 */
@Slf4j
@Service
public class AiProviderPresetService {

    private static final String PRESET_RESOURCE = "provider-presets.json";

    private final List<Map<String, Object>> presets;

    public AiProviderPresetService(ObjectMapper objectMapper) {
        this.presets = loadPresets(objectMapper);
    }

    /** 预置提供者列表；读取失败时为空列表。 */
    public List<Map<String, Object>> listPresets() {
        return presets;
    }

    private static List<Map<String, Object>> loadPresets(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(PRESET_RESOURCE).getInputStream()) {
            List<Map<String, Object>> loaded = objectMapper.readValue(in, new TypeReference<>() {});
            return List.copyOf(loaded);
        } catch (Exception e) {
            log.error("读取 {} 失败，AI 提供者列表降级为空", PRESET_RESOURCE, e);
            return List.of();
        }
    }
}
