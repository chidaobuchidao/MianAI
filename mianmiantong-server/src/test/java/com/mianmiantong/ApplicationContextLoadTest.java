package com.mianmiantong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ApplicationContext Load Test")
class ApplicationContextLoadTest {

    @Autowired
    private ApplicationContext ctx;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Spring context loads successfully")
    void contextLoads() {
        assertThat(ctx).isNotNull();
    }
}
