package com.mianmiantong.controller.paper;

import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.paper.PlagiarismReduceRequest;
import com.mianmiantong.service.paper.PlagiarismReduceService;
import com.mianmiantong.service.user.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/plagiarism-reduce")
@RequiredArgsConstructor
public class PlagiarismReduceController {

    private final PlagiarismReduceService plagiarismService;
    private final QuotaService quotaService;

    @PostMapping("/scan")
    public Map<String, Object> scanRepetition(@RequestBody PlagiarismReduceRequest body) {
        String text = body.getText();
        String source = body.getSourceText() != null ? body.getSourceText() : "";
        var repetition = plagiarismService.detectRepetitive(text);
        var similarity = source.isEmpty() ? null : plagiarismService.compareSimilarity(text, source);
        var citations = plagiarismService.checkCitations(text);
        var overlap = source.isEmpty() ? null : plagiarismService.calculateOverlap(text, source);
        double simulatedRate = plagiarismService.calculateSimulatedRate(repetition, similarity, overlap, text);
        return Map.of(
            "repetition", repetition,
            "similarity", similarity != null ? similarity : Map.of(),
            "citations", citations,
            "overlap", overlap != null ? overlap : Map.of(),
            "simulatedRate", simulatedRate
        );
    }

    @PostMapping("/run")
    public SseEmitter reduce(@RequestBody PlagiarismReduceRequest request) {
        quotaService.checkAndConsume(JwtAuthFilter.getCurrentUserId(), request.getModel());
        return plagiarismService.reduce(
            request.getText(),
            request.getSourceText(),
            request.getMode(),
            request.getModel(),
            request.getAnnotations()
        );
    }
}
