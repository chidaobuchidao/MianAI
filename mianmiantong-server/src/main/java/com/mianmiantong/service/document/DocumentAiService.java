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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DocumentAiService {

    private static final int CONVERT_MAX_ATTEMPTS = 36;
    private static final Duration CONVERT_POLL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(60);

    @Value("${aliyun.docmind.endpoint}")
    private String endpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Client createClient() throws Exception {
        String accessKeyId = getCredential("ALIBABA_CLOUD_ACCESS_KEY_ID");
        String accessKeySecret = getCredential("ALIBABA_CLOUD_ACCESS_KEY_SECRET");

        if (accessKeyId == null || accessKeySecret == null) {
            throw new RuntimeException("阿里云凭证未配置，请在 .env 中设置 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        }

        log.info("阿里云DocMind客户端创建中: endpoint={}, accessKeyId前缀={}",
                endpoint, accessKeyId.substring(0, Math.min(7, accessKeyId.length())));

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
            String cause = e.getCause() != null ? e.getCause().toString() : "";
            log.error("文档解析任务提交失败: fileName={}, 错误类型={}, 详情={}, 根因={}",
                    fileName, e.getClass().getSimpleName(), e.getMessage(), cause);
            throw new RuntimeException("文档解析提交失败: " + e.getMessage(), e);
        }
    }

    public String submitPdfToWord(Client client, InputStream fileStream, String fileName) {
        try {
            SubmitConvertPdfToWordJobAdvanceRequest request = new SubmitConvertPdfToWordJobAdvanceRequest();
            request.fileUrlObject = fileStream;
            request.fileName = fileName;

            RuntimeOptions runtime = new RuntimeOptions();
            SubmitConvertPdfToWordJobResponse response = client.submitConvertPdfToWordJobAdvance(request, runtime);
            SubmitConvertPdfToWordJobResponseBody body = response == null ? null : response.getBody();

            if (body == null || body.getData() == null || body.getData().getId() == null || body.getData().getId().isBlank()) {
                log.error("PDF convert submit rejected: fileName={}, code={}, message={}, requestId={}, rawBody={}",
                    fileName,
                    body == null ? null : body.getCode(),
                    body == null ? null : body.getMessage(),
                    body == null ? null : body.getRequestId(),
                    toJsonQuietly(body));
                throw new RuntimeException(buildSubmitConvertError(body));
            }

            String taskId = body.getData().getId();
            log.info("PDF转Word任务提交成功: fileName={}, taskId={}", fileName, taskId);
            return taskId;
        } catch (Exception e) {
            log.error("PDF转Word任务提交失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new RuntimeException("PDF转Word提交失败: " + e.getMessage(), e);
        }
    }

    public byte[] convertPdfToWord(InputStream fileStream, String fileName) {
        try {
            Client client = createClient();
            String taskId = submitPdfToWord(client, fileStream, fileName);
            String docxUrl = waitForConvertedDocumentUrl(client, taskId);
            return downloadConvertedDocument(docxUrl);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("PDF转Word初始化失败: " + e.getMessage(), e);
        }
    }

    private String waitForConvertedDocumentUrl(Client client, String taskId) {
        for (int attempt = 1; attempt <= CONVERT_MAX_ATTEMPTS; attempt++) {
            try {
                RuntimeOptions runtime = new RuntimeOptions();
                GetDocumentConvertResultRequest request = new GetDocumentConvertResultRequest();
                request.setId(taskId);

                GetDocumentConvertResultResponse response = client.getDocumentConvertResultWithOptions(request, runtime);
                GetDocumentConvertResultResponseBody body = response.getBody();

                if (body == null) {
                    log.warn("PDF convert result returned empty body: taskId={}, attempt={}/{}",
                        taskId, attempt, CONVERT_MAX_ATTEMPTS);
                    sleepBeforeNextPoll();
                    continue;
                }

                boolean completed = Boolean.TRUE.equals(body.getCompleted());

                if (isFailedConvertStatus(body) && completed) {
                    log.error("PDF convert task failed: taskId={}, code={}, status={}, message={}, requestId={}, rawBody={}",
                        taskId, body.getCode(), body.getStatus(), body.getMessage(), body.getRequestId(), toJsonQuietly(body));
                    throw new RuntimeException(buildConvertResultError(body));
                }

                if (completed && body.getData() != null && !body.getData().isEmpty()) {
                    String url = body.getData().get(0).getUrl();
                    if (url != null && !url.isBlank()) {
                        log.info("PDF转Word完成: taskId={}, attempts={}, url={}", taskId, attempt, url);
                        return url;
                    }
                }

                log.info("PDF转Word处理中: taskId={}, status={}, attempt={}/{}",
                    taskId, body.getStatus(), attempt, CONVERT_MAX_ATTEMPTS);
                sleepBeforeNextPoll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("PDF转Word轮询被中断", e);
            } catch (Exception e) {
                log.warn("PDF转Word结果查询失败: taskId={}, attempt={}, error={}",
                    taskId, attempt, e.getMessage());
                if (attempt == CONVERT_MAX_ATTEMPTS) {
                    throw new RuntimeException("PDF转Word结果查询失败: " + e.getMessage(), e);
                }
            }
        }
        throw new RuntimeException("PDF转Word处理超时，请稍后重试");
    }

    private void sleepBeforeNextPoll() throws InterruptedException {
        Thread.sleep(CONVERT_POLL_INTERVAL.toMillis());
    }

    private boolean isFailedConvertStatus(GetDocumentConvertResultResponseBody body) {
        return isFailureValue(body.getStatus()) || isFailureValue(body.getCode());
    }

    private boolean isFailureValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.contains("fail") || normalized.contains("error");
    }

    private String buildSubmitConvertError(SubmitConvertPdfToWordJobResponseBody body) {
        if (body == null) {
            return "PDF转Word提交失败: 阿里云返回空响应";
        }
        return "PDF转Word提交失败: code=" + safeText(body.getCode())
            + ", message=" + safeText(body.getMessage())
            + ", requestId=" + safeText(body.getRequestId());
    }

    private String buildConvertResultError(GetDocumentConvertResultResponseBody body) {
        return "PDF转Word处理失败: code=" + safeText(body.getCode())
            + ", status=" + safeText(body.getStatus())
            + ", message=" + safeText(body.getMessage())
            + ", requestId=" + safeText(body.getRequestId());
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private String toJsonQuietly(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private byte[] downloadConvertedDocument(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(DOWNLOAD_TIMEOUT)
                .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("下载转换结果失败: HTTP " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("下载PDF转Word结果失败: url={}, error={}", url, e.getMessage(), e);
            throw new RuntimeException("下载PDF转Word结果失败: " + e.getMessage(), e);
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
