package com.mianmiantong.controller.paper;

import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.paper.PolishRequest;
import com.mianmiantong.service.paper.PolishService;
import com.mianmiantong.service.user.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/polish")
@RequiredArgsConstructor
public class PolishController {

    private final PolishService polishService;
    private final QuotaService quotaService;

    @PostMapping("/run")
    public SseEmitter runPolish(@RequestBody PolishRequest request) {
        quotaService.checkAndConsume(JwtAuthFilter.getCurrentUserId(), request.getModel());
        return polishService.runPolish(request);
    }

    @PostMapping("/scan")
    public Map<String, Object> scanFormat(@RequestBody Map<String, String> body) {
        return Map.of("result", polishService.scanFormat(body.get("text")));
    }
}
