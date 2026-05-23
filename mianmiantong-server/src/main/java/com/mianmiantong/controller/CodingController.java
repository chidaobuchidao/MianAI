package com.mianmiantong.controller;

import com.mianmiantong.dto.coding.RunCodeRequest;
import com.mianmiantong.service.coding.CodingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/coding")
public class CodingController {

    private final CodingService codingService;

    public CodingController(CodingService codingService) {
        this.codingService = codingService;
    }

    @PostMapping("/run")
    public Map<String, Object> runCode(@RequestBody RunCodeRequest request) {
        return codingService.runCode(request.getCode(), request.getLanguage(), request.getStdin());
    }
}
