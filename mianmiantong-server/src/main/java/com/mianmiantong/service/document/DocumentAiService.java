package com.mianmiantong.service.document;

import com.aliyun.docmind_api20220711.Client;
import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
public class DocumentAiService {

    @Value("${aliyun.docmind.endpoint}")
    private String endpoint;

    private Client createClient() throws Exception {
        com.aliyun.credentials.Client credentialClient =
            new com.aliyun.credentials.Client();
        Config config = new Config()
            .setAccessKeyId(credentialClient.getAccessKeyId())
            .setAccessKeySecret(credentialClient.getAccessKeySecret());
        config.endpoint = endpoint;
        return new Client(config);
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
     * 查询解析结果
     */
    public DocumentParseResult getResult(String taskId) {
        try {
            Client client = createClient();
            GetDocParserResultRequest request = new GetDocParserResultRequest();
            request.setId(taskId);

            RuntimeOptions runtime = new RuntimeOptions();
            GetDocParserResultResponse response = client.getDocParserResultWithOptions(request, runtime);

            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) response.getBody().getData();
            String status = data.get("status");
            DocumentParseResult result = DocumentParseResult.builder()
                .taskId(taskId)
                .status(status)
                .build();

            if ("SUCCESS".equals(status)) {
                result.setParsedText(data.get("content"));
                log.info("文档解析完成: taskId={}, textLength={}", taskId,
                    result.getParsedText() != null ? result.getParsedText().length() : 0);
            } else if ("FAIL".equals(status)) {
                result.setErrorMessage("文档解析失败");
                log.warn("文档解析失败: taskId={}", taskId);
            }

            return result;
        } catch (Exception e) {
            log.error("查询解析结果失败: taskId={}", taskId, e);
            throw new RuntimeException("查询解析结果失败: " + e.getMessage());
        }
    }
}
