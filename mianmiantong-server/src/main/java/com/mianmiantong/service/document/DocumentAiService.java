package com.mianmiantong.service.document;

import com.aliyun.docmind_api20220711.Client;
import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DocumentAiService {

    @Value("${aliyun.docmind.endpoint}")
    private String endpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Client createClient() throws Exception {
        String accessKeyId = getCredential("ALIBABA_CLOUD_ACCESS_KEY_ID");
        String accessKeySecret = getCredential("ALIBABA_CLOUD_ACCESS_KEY_SECRET");

        if (accessKeyId == null || accessKeySecret == null) {
            throw new RuntimeException("阿里云凭证未配置，请在 .env 中设置 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        }

        Config config = new Config()
            .setAccessKeyId(accessKeyId)
            .setAccessKeySecret(accessKeySecret);
        config.endpoint = endpoint;
        return new Client(config);
    }

    /** 先从系统属性读，再从环境变量读 */
    private String getCredential(String key) {
        String val = System.getProperty(key);
        if (val != null && !val.isBlank()) return val;
        val = System.getenv(key);
        return val;
    }

    /**
     * 提交文档解析任务，返回 taskId
     */
    public String submitParse(InputStream fileStream, String fileName) {
        try {
            Client client = createClient();
            SubmitDocParserJobAdvanceRequest request = new SubmitDocParserJobAdvanceRequest();
            request.fileUrlObject = fileStream;
            request.fileName = fileName;

            RuntimeOptions runtime = new RuntimeOptions();
            SubmitDocParserJobResponse response = client.submitDocParserJobAdvance(request, runtime);

            String taskId = response.getBody().getData().getId();
            log.info("文档解析任务提交成功: fileName={}, taskId={}", fileName, taskId);
            return taskId;
        } catch (Exception e) {
            log.error("文档解析任务提交失败: fileName={}", fileName, e);
            throw new RuntimeException("文档解析提交失败: " + e.getMessage());
        }
    }

    /**
     * 查询解析结果（大模型版需分页查询 Layout）
     * API 返回的 Data 是已反序列化的对象（包含 layouts 列表），而非 JSON 字符串
     */
    @SuppressWarnings("unchecked")
    public DocumentParseResult getResult(String taskId) {
        try {
            Client client = createClient();
            RuntimeOptions runtime = new RuntimeOptions();

            StringBuilder allText = new StringBuilder();
            int layoutNum = 0;
            int layoutStepSize = 100;
            boolean hasMore;

            do {
                GetDocParserResultRequest request = new GetDocParserResultRequest();
                request.setId(taskId);
                request.setLayoutNum(layoutNum);
                request.setLayoutStepSize(layoutStepSize);

                GetDocParserResultResponse response = client.getDocParserResultWithOptions(request, runtime);
                Object dataObj = response.getBody().getData();

                // Data 为 null → 仍在解析中
                if (dataObj == null) {
                    return DocumentParseResult.builder().taskId(taskId).status("PROCESSING").build();
                }

                Map<String, Object> data = objectMapper.convertValue(dataObj, Map.class);
                List<Map<String, Object>> layouts = (List<Map<String, Object>>) data.get("layouts");

                if (layouts == null || layouts.isEmpty()) {
                    return DocumentParseResult.builder().taskId(taskId).status("PROCESSING").build();
                }

                for (Map<String, Object> layout : layouts) {
                    String markdown = (String) layout.get("markdownContent");
                    if (markdown != null && !markdown.isBlank()) {
                        allText.append(markdown).append("\n");
                    }
                }

                hasMore = layouts.size() >= layoutStepSize;
                layoutNum += layoutStepSize;

            } while (hasMore);

            String parsedText = allText.toString().trim();
            if (parsedText.isEmpty()) {
                return DocumentParseResult.builder().taskId(taskId).status("PROCESSING").build();
            }

            log.info("文档解析完成: taskId={}, textLength={}", taskId, parsedText.length());
            return DocumentParseResult.builder()
                .taskId(taskId)
                .status("SUCCESS")
                .parsedText(parsedText)
                .build();

        } catch (Exception e) {
            log.error("查询解析结果失败: taskId={}", taskId, e);
            throw new RuntimeException("查询解析结果失败: " + e.getMessage());
        }
    }
}
