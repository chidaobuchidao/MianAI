package com.mianmiantong.controller.paper;

import com.mianmiantong.dto.paper.EvidenceRequest;
import com.mianmiantong.dto.paper.EvidenceResponse;
import com.mianmiantong.service.paper.EvidenceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paper-kb")
public class PaperKbController {

    private final EvidenceService evidenceService;

    public PaperKbController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping("/evidence")
    public EvidenceResponse classifyEvidence(@RequestBody EvidenceRequest request) {
        return evidenceService.classify(request);
    }
}
