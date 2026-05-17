package com.mianmiantong.service.coding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class CodingService {

    @Value("${PISTON_API_URL:http://localhost:2000}")
    private String pistonUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> runCode(String code, String language) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("language", getPistonLanguage(language));
            body.put("version", getPistonVersion(language));
            body.put("files", List.of(Map.of("name", getFileName(language), "content", code)));
            body.put("run_timeout", 3000);

            String json = mapper.writeValueAsString(body);

            var url = URI.create(pistonUrl + "/api/v2/execute").toURL();
            var conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            var inputStream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            if (response != null && !response.isEmpty()) {
                return mapper.readValue(response, Map.class);
            }
            return Map.of("error", "空响应, HTTP " + status);
        } catch (Exception e) {
            return Map.of("error", "执行失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private String getFileName(String lang) {
        return switch (lang) {
            case "java" -> "Main.java";
            case "python" -> "main.py";
            case "javascript", "js" -> "main.js";
            case "cpp", "c" -> "main.cpp";
            case "go" -> "main.go";
            default -> "code.txt";
        };
    }

    private String getPistonLanguage(String lang) {
        return switch (lang) {
            case "java" -> "java";
            case "python" -> "python";
            case "javascript", "js" -> "javascript";
            case "cpp", "c++" -> "c++";
            case "c" -> "c";
            case "go" -> "go";
            default -> lang;
        };
    }

    private String getPistonVersion(String lang) {
        return switch (lang) {
            case "java" -> "15.0.2";
            case "python" -> "3.10.0";
            case "javascript", "js" -> "18.15.0";
            case "cpp", "c++", "c" -> "10.2.0";
            case "go" -> "1.16.2";
            default -> "*";
        };
    }
}
