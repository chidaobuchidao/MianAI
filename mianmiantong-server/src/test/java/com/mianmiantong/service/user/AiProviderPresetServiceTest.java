package com.mianmiantong.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiProviderPresetService")
class AiProviderPresetServiceTest {

    @Test
    @DisplayName("从 classpath 加载预置提供者列表")
    void loadsPresetsFromClasspath() {
        AiProviderPresetService service = new AiProviderPresetService(new ObjectMapper());

        List<Map<String, Object>> presets = service.listPresets();

        assertThat(presets).isNotEmpty();
        assertThat(presets).allSatisfy(preset -> assertThat(preset).isNotEmpty());
    }

    @Test
    @DisplayName("重复调用返回同一份缓存结果，不重读文件")
    void cachesPresetsAcrossCalls() {
        AiProviderPresetService service = new AiProviderPresetService(new ObjectMapper());

        assertThat(service.listPresets()).isSameAs(service.listPresets());
    }

    @Test
    @DisplayName("返回的列表不可变，调用方无法污染缓存")
    void presetsAreImmutable() {
        AiProviderPresetService service = new AiProviderPresetService(new ObjectMapper());

        assertThat(service.listPresets().getClass().getName())
            .startsWith("java.util.ImmutableCollections");
    }
}
