package com.mianmiantong.controller.paper;

import com.mianmiantong.dto.paper.PlagiarismReduceRequest;
import com.mianmiantong.service.paper.PlagiarismReduceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/plagiarism-reduce")
@RequiredArgsConstructor
public class PlagiarismReduceController {

    private final PlagiarismReduceService plagiarismService;

    @PostMapping("/scan")
    public Map<String, Object> scanRepetition(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String source = body.getOrDefault("sourceText", "");
        var repetition = plagiarismService.detectRepetitive(text);
        var similarity = source.isEmpty() ? null : plagiarismService.compareSimilarity(text, source);
        var citations = plagiarismService.checkCitations(text);
        return Map.of(
            "repetition", repetition,
            "similarity", similarity != null ? similarity : Map.of(),
            "citations", citations
        );
    }

    @PostMapping("/run")
    public SseEmitter reduce(@RequestBody PlagiarismReduceRequest request) {
        return plagiarismService.reduce(
            request.getText(),
            request.getSourceText(),
            request.getMode()
        );
    }
}
