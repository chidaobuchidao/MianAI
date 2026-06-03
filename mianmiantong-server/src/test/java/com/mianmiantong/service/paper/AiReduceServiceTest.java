package com.mianmiantong.service.paper;

import com.mianmiantong.service.ai.gateway.AiGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiReduceService Smoke Test")
class AiReduceServiceTest {

    @Mock
    private AiGateway aiGateway;

    @Test
    @DisplayName("service can be instantiated with mocked AiGateway")
    void canInstantiate() {
        AiReduceService service = new AiReduceService(aiGateway);
        assertThat(service).isNotNull();
    }
}
