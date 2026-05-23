package com.mianmiantong.service.coding;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.entity.coding.AlgorithmProblem;
import com.mianmiantong.mapper.coding.AlgorithmProblemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class AlgorithmProblemService {

    private final AlgorithmProblemMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlgorithmProblemService(AlgorithmProblemMapper mapper) {
        this.mapper = mapper;
    }

    /** 根据岗位随机选题，返回 [编程题目]JSON 字符串（含所有语言模板） */
    public String generateCodingProblem(String position) {
        String lang = mapPositionToLanguage(position);
        AlgorithmProblem problem = getRandomProblem();

        if (problem == null) {
            return fallbackProblem(lang);
        }

        Map<String, String> allTemplates = extractAllStarterCodes(problem.getStarterCode());
        String template = allTemplates.getOrDefault(lang, allTemplates.values().iterator().next());
        if (!allTemplates.containsKey(lang)) lang = allTemplates.keySet().iterator().next();

        return formatCodingProblem(problem, template, lang, allTemplates);
    }

    /** 根据语言获取题库中该语言的模板 */
    public Map<String, String> getTemplatesForProblem(Long problemId) {
        AlgorithmProblem problem = mapper.selectById(problemId);
        if (problem == null) return Map.of();
        return extractAllStarterCodes(problem.getStarterCode());
    }

    private String mapPositionToLanguage(String position) {
        if (position == null) return "java";
        if (position.contains("前端")) return "javascript";
        if (position.contains("数据") || position.contains("DevOps")) return "python";
        return "java";
    }

    private AlgorithmProblem getRandomProblem() {
        Long count = mapper.selectCount(null);
        if (count == null || count == 0) return null;
        int offset = ThreadLocalRandom.current().nextInt(count.intValue());
        return mapper.selectOne(
            new LambdaQueryWrapper<AlgorithmProblem>()
                .last("LIMIT 1 OFFSET " + offset)
        );
    }

    private Map<String, String> extractAllStarterCodes(String starterCodeJson) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            JsonNode node = objectMapper.readTree(starterCodeJson);
            var it = node.fields();
            while (it.hasNext()) {
                var entry = it.next();
                result.put(entry.getKey(), entry.getValue().asText());
            }
        } catch (Exception e) {
            log.warn("解析starter_code JSON失败: {}", e.getMessage());
        }
        return result;
    }

    private String formatCodingProblem(AlgorithmProblem problem, String template, String lang,
                                       Map<String, String> allTemplates) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "algorithm");
        data.put("title", problem.getTitle());
        data.put("description", problem.getDescription());
        data.put("template", template);
        data.put("language", lang);
        data.put("templates", allTemplates);
        try {
            data.put("testCases", objectMapper.readTree(problem.getTestCases()));
        } catch (Exception e) {
            data.put("testCases", List.of());
        }

        try {
            return "[编程题目]" + objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "[编程题目]" + "{\"error\":\"题目格式化失败\"}";
        }
    }

    private String fallbackProblem(String lang) {
        String javaCode = "public class Solution {\n    public void solve() {\n        // TODO: 实现代码\n    }\n\n    public static void main(String[] args) {\n        new Solution().solve();\n    }\n}";
        String pyCode = "def solution(input_data=None):\n    # TODO: 实现代码\n    pass\n\nif __name__ == \"__main__\":\n    print(solution(input()))";
        String jsCode = "function solution(input) {\n    // TODO: 实现代码\n}\n\nconst readline = require('readline');\nconst rl = readline.createInterface({ input: process.stdin });\nrl.on('line', line => { console.log(solution(line)); rl.close(); });";
        try {
            return "[编程题目]" + objectMapper.writeValueAsString(Map.of(
                "type", "algorithm",
                "title", "编程练习",
                "description", "请实现一个解法函数",
                "template", javaCode,
                "language", "java",
                "templates", Map.of("java", javaCode, "python", pyCode, "javascript", jsCode),
                "testCases", List.of(Map.of("input", "1", "expected", "1"))
            ));
        } catch (Exception e) {
            return "[编程题目]{\"error\":\"题目生成失败\"}";
        }
    }
}
