package com.mianmiantong.controller.paper;

import com.mianmiantong.dto.paper.AiReduceRequest;
import com.mianmiantong.service.paper.AiReduceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-reduce")
@RequiredArgsConstructor
public class AiReduceController {

    private final AiReduceService aiReduceService;

    @PostMapping("/scan")
    public Map<String, Object> scanAiFeatures(@RequestBody Map<String, String> body) {
        return Map.of("result", aiReduceService.scanAiFeatures(body.get("text")));
    }

    @PostMapping("/rewrite")
    public SseEmitter rewrite(@RequestBody AiReduceRequest request) {
        return aiReduceService.rewrite(request.getText(), request.getMode());
    }
}
