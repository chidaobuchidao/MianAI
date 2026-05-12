# 简历优化功能 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现简历上传、阿里云文档智能解析、AI 多维分析优化、面试联动的完整功能

**Architecture:** 新增 `service/resume/`、`service/document/`、`controller/resume/` 模块，复用现有 `service/ai/` AI 调用管道和 SSE 流式机制。现有文件按 domain 分包迁移到 controller/dto/entity/mapper/service 的各子包

**Tech Stack:** Spring Boot 3.2.0 / MyBatis-Plus 3.5.5 / 阿里云文档智能 API / DeepSeek SSE 流式 / uni-app Vue3

---

## Phase 1: 文件目录重构

### Task 1: 迁移 Controller 层到 domain 子包

**Files:**
- Move: `controller/AuthController.java` → `controller/auth/AuthController.java`
- Move: `controller/QuestionController.java` → `controller/question/QuestionController.java`
- Move: `controller/ExamController.java` → `controller/exam/ExamController.java`
- Move: `controller/InterviewController.java` → `controller/interview/InterviewController.java`
- Move: `controller/WrongQuestionController.java` → `controller/wrongbook/WrongQuestionController.java`
- Move: `controller/UserController.java` → `controller/user/UserController.java`
- Move: `controller/AnswerController.java` → `controller/answer/AnswerController.java`

- [ ] **Step 1: 创建目标子包目录**

```bash
mkdir -p src/main/java/com/mianmiantong/controller/{auth,question,exam,interview,wrongbook,user,answer}
```

- [ ] **Step 2: 移动文件并更新 package 声明**

移动 `AuthController.java` → `controller/auth/`，修改 package:
```java
package com.mianmiantong.controller.auth;
```

移动 `QuestionController.java` → `controller/question/`:
```java
package com.mianmiantong.controller.question;
```

移动 `ExamController.java` → `controller/exam/`:
```java
package com.mianmiantong.controller.exam;
```

移动 `InterviewController.java` → `controller/interview/`:
```java
package com.mianmiantong.controller.interview;
```

移动 `WrongQuestionController.java` → `controller/wrongbook/`:
```java
package com.mianmiantong.controller.wrongbook;
```

移动 `UserController.java` → `controller/user/`:
```java
package com.mianmiantong.controller.user;
```

移动 `AnswerController.java` → `controller/answer/`:
```java
package com.mianmiantong.controller.answer;
```

- [ ] **Step 3: 更新所有 import 引用其它迁移文件的语句**

`InterviewController.java` 中 import 改为:
```java
import com.mianmiantong.dto.interview.InterviewAnswerRequest;
import com.mianmiantong.dto.interview.InterviewStartRequest;
import com.mianmiantong.service.interview.InterviewService;
```

`ExamController.java` 中 import 改为:
```java
import com.mianmiantong.dto.exam.ExamSubmitRequest;
import com.mianmiantong.service.exam.ExamService;
```

- [ ] **Step 4: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mianmiantong/controller/
git commit -m "refactor: migrate controllers to domain sub-packages"
```

---

### Task 2: 迁移 DTO 层到 domain 子包

**Files:**
- Move: `dto/LoginRequest.java`, `dto/LoginResponse.java` → `dto/auth/`
- Move: `dto/PageQuery.java` → `dto/question/`
- Move: `dto/ExamSubmitRequest.java` → `dto/exam/`
- Move: `dto/InterviewStartRequest.java`, `dto/InterviewAnswerRequest.java` → `dto/interview/`
- Move: `dto/UserAiConfigRequest.java` → `dto/user/`
- Move: `dto/AnswerSubmitRequest.java` → `dto/answer/`

- [ ] **Step 1: 创建目标子包 + 移动文件 + 更新 package**

```bash
mkdir -p src/main/java/com/mianmiantong/dto/{auth,question,exam,interview,user,answer}
```

`LoginRequest.java` → `dto/auth/`:
```java
package com.mianmiantong.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "code不能为空")
    private String code;
    private String nickname;
    private String avatarUrl;
}
```

`LoginResponse.java` → `dto/auth/`:
```java
package com.mianmiantong.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String nickname;
    private String avatarUrl;
}
```

`PageQuery.java` → `dto/question/`:
```java
package com.mianmiantong.dto.question;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page;
    private Integer size;
}
```

`ExamSubmitRequest.java` → `dto/exam/`:
```java
package com.mianmiantong.dto.exam;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ExamSubmitRequest {
    @NotEmpty
    private List<ExamAnswer> answers;

    @Data
    public static class ExamAnswer {
        private Long questionId;
        private String userAnswer;
    }
}
```

`InterviewStartRequest.java` → `dto/interview/`:
```java
package com.mianmiantong.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewStartRequest {
    @NotBlank
    private String position;
}
```

`InterviewAnswerRequest.java` → `dto/interview/`:
```java
package com.mianmiantong.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewAnswerRequest {
    @NotBlank
    private String answer;
}
```

`UserAiConfigRequest.java` → `dto/user/`:
```java
package com.mianmiantong.dto.user;

import lombok.Data;

@Data
public class UserAiConfigRequest {
    private String provider;
    private String apiKey;
}
```

`AnswerSubmitRequest.java` → `dto/answer/`:
```java
package com.mianmiantong.dto.answer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitRequest {
    @NotNull
    private Long questionId;
    @NotNull
    private String userAnswer;
}
```

- [ ] **Step 2: 更新所有引用这些 DTO 的 import**

Controller 和 Service 层所有引用旧路径的 import 改为新子包路径。例如:
```java
// AuthController.java
import com.mianmiantong.dto.auth.LoginRequest;

// InterviewController.java
import com.mianmiantong.dto.interview.InterviewStartRequest;
import com.mianmiantong.dto.interview.InterviewAnswerRequest;

// ExamController.java
import com.mianmiantong.dto.exam.ExamSubmitRequest;
```

- [ ] **Step 3: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/dto/
git commit -m "refactor: migrate DTOs to domain sub-packages"
```

---

### Task 3: 迁移 Entity 层到 domain 子包

**Files:**
- Move: `entity/User.java`, `entity/UserAiConfig.java`, `entity/UserFavorite.java` → `entity/user/`
- Move: `entity/Question.java`, `entity/QuestionCategory.java` → `entity/question/`
- Move: `entity/Exam.java`, `entity/ExamQuestion.java`, `entity/AnswerRecord.java` → `entity/exam/`
- Move: `entity/InterviewSession.java` → `entity/interview/`
- Move: `entity/WrongQuestion.java` → `entity/wrongbook/`

- [ ] **Step 1: 创建子包 + 移动 + 更新 package**

```bash
mkdir -p src/main/java/com/mianmiantong/entity/{user,question,exam,interview,wrongbook}
```

每个文件修改 package 声明，例如 `User.java` → `entity/user/`:
```java
package com.mianmiantong.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private Integer role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

`Question.java` → `entity/question/`:
```java
package com.mianmiantong.entity.question;
// ... (内容不变，仅改 package)
```

`Exam.java` → `entity/exam/`:
```java
package com.mianmiantong.entity.exam;
// ...
```

`InterviewSession.java` → `entity/interview/`:
```java
package com.mianmiantong.entity.interview;
// ...
```

`WrongQuestion.java` → `entity/wrongbook/`:
```java
package com.mianmiantong.entity.wrongbook;
// ...
```

- [ ] **Step 2: 更新 MapStruct/Mapper/Service 中所有 entity import**

- [ ] **Step 3: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/entity/
git commit -m "refactor: migrate entities to domain sub-packages"
```

---

### Task 4: 迁移 Mapper 层到 domain 子包

- [ ] **Step 1: 创建子包 + 移动 + 更新 package**

```bash
mkdir -p src/main/java/com/mianmiantong/mapper/{user,question,exam,interview,wrongbook}
```

所有 Mapper 类更新 package 和 entity import。例如 `UserMapper.java` → `mapper/user/`:
```java
package com.mianmiantong.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.entity.user.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

`QuestionMapper.java` → `mapper/question/`:
```java
package com.mianmiantong.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.entity.question.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
```

- [ ] **Step 2: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mianmiantong/mapper/
git commit -m "refactor: migrate mappers to domain sub-packages"
```

---

### Task 5: 迁移 Service 层到 domain 子包

- [ ] **Step 1: 创建子包 + 移动 + 更新 package 和所有 import**

```bash
mkdir -p src/main/java/com/mianmiantong/service/{auth,question,exam,interview,wrongbook,user,answer}
```

`AuthService.java` → `service/auth/`:
```java
package com.mianmiantong.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.common.JwtUtil;
import com.mianmiantong.dto.auth.LoginRequest;
import com.mianmiantong.dto.auth.LoginResponse;
import com.mianmiantong.entity.user.User;
import com.mianmiantong.mapper.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        String openid = mockWechatLogin(request.getCode());

        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(request.getNickname() != null ? request.getNickname() : "用户" + System.currentTimeMillis() % 10000);
            user.setAvatarUrl(request.getAvatarUrl());
            userMapper.insert(user);
            log.info("新用户注册: openid={}, userId={}", openid, user.getId());
        }

        String token = jwtUtil.generateToken(user.getId(), openid);
        return new LoginResponse(token, user.getId(), user.getNickname(), user.getAvatarUrl());
    }

    private String mockWechatLogin(String code) {
        if (code != null && code.length() > 20) {
            return "wx_" + code.substring(0, 28);
        }
        return "wx_test_openid_" + (code != null ? code.hashCode() & 0x7fffffff : System.currentTimeMillis() % 10000);
    }
}
```

`ExamService.java` → `service/exam/` — 更新 import:
```java
package com.mianmiantong.service.exam;

import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.dto.exam.ExamSubmitRequest;
import com.mianmiantong.entity.exam.*;
import com.mianmiantong.entity.question.Question;
import com.mianmiantong.entity.question.QuestionCategory;
import com.mianmiantong.entity.wrongbook.WrongQuestion;
import com.mianmiantong.mapper.exam.*;
import com.mianmiantong.mapper.question.*;
import com.mianmiantong.mapper.wrongbook.*;
// ... rest unchanged
```

`InterviewService.java` → `service/interview/` — 更新 import:
```java
package com.mianmiantong.service.interview;

import com.mianmiantong.entity.interview.InterviewSession;
import com.mianmiantong.entity.user.UserAiConfig;
import com.mianmiantong.mapper.interview.InterviewSessionMapper;
import com.mianmiantong.service.ai.AiService;
import com.mianmiantong.service.user.UserAiConfigService;
// ... rest unchanged
```

同样处理其余 Service：QuestionService、AnswerService、WrongQuestionService、UserAiConfigService

- [ ] **Step 2: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```
Expected: BUILD SUCCESS。若出现 import 报错，逐文件修正

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mianmiantong/service/
git commit -m "refactor: migrate services to domain sub-packages"
```

---

## Phase 2: 数据库迁移 + 实体

### Task 6: 创建数据库迁移 V4 — resume 表

**Files:**
- Create: `src/main/resources/db/migration/V4__resume.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- V4__resume.sql - 简历表
CREATE TABLE resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型 pdf/docx/jpg/png',
    file_size BIGINT COMMENT '文件大小(字节)',
    job_description TEXT NOT NULL COMMENT '目标岗位JD',
    position VARCHAR(50) COMMENT '目标岗位名称',
    parsed_text MEDIUMTEXT COMMENT '解析后的结构化文本',
    parse_status TINYINT DEFAULT 0 COMMENT '0解析中 1完成 -1失败',
    doc_task_id VARCHAR(100) COMMENT '阿里云任务ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';
```

- [ ] **Step 2: 创建 resume_analysis 迁移 V5**

**Files:**
- Create: `src/main/resources/db/migration/V5__resume_analysis.sql`

```sql
-- V5__resume_analysis.sql - 简历分析报告表
CREATE TABLE resume_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resume_id BIGINT NOT NULL UNIQUE COMMENT '简历ID',
    overall_score INT COMMENT '综合评分1-10',
    dimensions JSON COMMENT '五维度评分 [{name,score,comment}]',
    missing_keywords JSON COMMENT '缺失关键词 ["Redis","Docker"]',
    optimized_text MEDIUMTEXT COMMENT '优化后完整简历(Markdown)',
    highlights JSON COMMENT '优化对比 [{section,before,after,reason}]',
    interview_questions JSON COMMENT '面试追问 ["问题1","问题2"]',
    suggestion TEXT COMMENT '总体提升建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    INDEX idx_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历分析报告表';
```

- [ ] **Step 3: 启用 Flyway 验证建表**

```bash
# 临时启用 flyway 执行迁移后禁用
cd mianmiantong-server
# 修改 application.yml: flyway.enabled: true
mvn spring-boot:run -q &
sleep 15
curl -s http://localhost:8080/doc.html | head -1
# 确认 resume/resume_analysis 表已创建
# 改回 flyway.enabled: false
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V4__resume.sql
git add src/main/resources/db/migration/V5__resume_analysis.sql
git commit -m "feat: add resume and resume_analysis tables"
```

---

### Task 7: 创建 Resume 和 ResumeAnalysis 实体

**Files:**
- Create: `entity/resume/Resume.java`
- Create: `entity/resume/ResumeAnalysis.java`

- [ ] **Step 1: 编写 Resume.java**

```java
package com.mianmiantong.entity.resume;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String jobDescription;
    private String position;
    private String parsedText;
    private Integer parseStatus;
    private String docTaskId;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 编写 ResumeAnalysis.java**

```java
package com.mianmiantong.entity.resume;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resume_analysis")
public class ResumeAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resumeId;
    private Integer overallScore;
    private String dimensions;
    private String missingKeywords;
    private String optimizedText;
    private String highlights;
    private String interviewQuestions;
    private String suggestion;
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 创建 Mapper 接口**

**Files:**
- Create: `mapper/resume/ResumeMapper.java`
- Create: `mapper/resume/ResumeAnalysisMapper.java`

```java
// ResumeMapper.java
package com.mianmiantong.mapper.resume;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.entity.resume.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
```

```java
// ResumeAnalysisMapper.java
package com.mianmiantong.mapper.resume;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianmiantong.entity.resume.ResumeAnalysis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeAnalysisMapper extends BaseMapper<ResumeAnalysis> {
}
```

- [ ] **Step 4: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mianmiantong/entity/resume/
git add src/main/java/com/mianmiantong/mapper/resume/
git commit -m "feat: add Resume and ResumeAnalysis entities with mappers"
```

---

## Phase 3: 阿里云文档智能集成

### Task 8: 添加 Maven 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 pom.xml 添加阿里云 SDK 依赖**

```xml
<!-- 阿里云文档智能 -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>docmind_api20220711</artifactId>
    <version>1.3.1</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>credentials-java</artifactId>
    <version>0.3.5</version>
</dependency>
```

- [ ] **Step 2: 添加配置项到 application.yml**

```yaml
aliyun:
  docmind:
    endpoint: docmind-api.cn-hangzhou.aliyuncs.com
```

- [ ] **Step 3: 验证依赖下载**

```bash
cd mianmiantong-server && mvn dependency:resolve -q
```

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/main/resources/application.yml
git commit -m "feat: add Aliyun DocMind SDK dependencies"
```

---

### Task 9: 实现 DocumentAiService

**Files:**
- Create: `service/document/DocumentParseResult.java`
- Create: `service/document/DocumentAiService.java`

- [ ] **Step 1: 编写 DocumentParseResult DTO**

```java
package com.mianmiantong.service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResult {
    private String taskId;
    private String status;     // PROCESSING / SUCCESS / FAIL
    private String parsedText;
    private String errorMessage;
}
```

- [ ] **Step 2: 编写 DocumentAiService — 提交解析任务**

```java
package com.mianmiantong.service.document;

import com.aliyun.docmind_api20220711.Client;
import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

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

            String status = response.getBody().getData().getStatus();
            DocumentParseResult result = DocumentParseResult.builder()
                .taskId(taskId)
                .status(status)
                .build();

            if ("SUCCESS".equals(status)) {
                result.setParsedText(response.getBody().getData().getContent());
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
```

- [ ] **Step 3: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/service/document/
git commit -m "feat: add DocumentAiService for Aliyun DocMind integration"
```

---

## Phase 4: 简历 Service + Controller

### Task 10: 创建 ResumeService — 上传与解析任务管理

**Files:**
- Create: `service/resume/ResumeService.java`

- [ ] **Step 1: 编写 ResumeService**

```java
package com.mianmiantong.service.resume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianmiantong.config.JwtAuthFilter;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.entity.resume.ResumeAnalysis;
import com.mianmiantong.mapper.resume.ResumeAnalysisMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.document.DocumentAiService;
import com.mianmiantong.service.document.DocumentParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper analysisMapper;
    private final DocumentAiService documentAiService;

    public ResumeService(ResumeMapper resumeMapper,
                         ResumeAnalysisMapper analysisMapper,
                         DocumentAiService documentAiService) {
        this.resumeMapper = resumeMapper;
        this.analysisMapper = analysisMapper;
        this.documentAiService = documentAiService;
    }

    /**
     * 上传简历 — 保存记录并提交解析任务
     */
    @Transactional
    public Map<String, Object> upload(MultipartFile file, String jobDescription, String position) {
        Long userId = JwtAuthFilter.getCurrentUserId();

        String fileName = file.getOriginalFilename();
        String fileType = getFileType(fileName);

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFileName(fileName);
        resume.setFileType(fileType);
        resume.setFileSize(file.getSize());
        resume.setJobDescription(jobDescription);
        resume.setPosition(position);
        resume.setParseStatus(0);
        resumeMapper.insert(resume);

        // 异步提交解析任务（实际场景可放入队列）
        try {
            String taskId = documentAiService.submitParse(file.getInputStream(), fileName);
            resume.setDocTaskId(taskId);
            resumeMapper.updateById(resume);
        } catch (Exception e) {
            resume.setParseStatus(-1);
            resumeMapper.updateById(resume);
            throw new RuntimeException("文档解析提交失败", e);
        }

        log.info("简历上传成功: resumeId={}, fileName={}", resume.getId(), fileName);

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("taskId", resume.getDocTaskId());
        result.put("parseStatus", resume.getParseStatus());
        result.put("fileName", resume.getFileName());
        return result;
    }

    /**
     * 轮询解析状态
     */
    @Transactional
    public Map<String, Object> getStatus(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) throw new IllegalArgumentException("简历不存在");

        Map<String, Object> result = new HashMap<>();
        result.put("resumeId", resume.getId());
        result.put("parseStatus", resume.getParseStatus());

        if (resume.getParseStatus() == 0 && resume.getDocTaskId() != null) {
            // 轮询阿里云结果
            DocumentParseResult parseResult = documentAiService.getResult(resume.getDocTaskId());
            if ("SUCCESS".equals(parseResult.getStatus())) {
                resume.setParseStatus(1);
                resume.setParsedText(parseResult.getParsedText());
                resumeMapper.updateById(resume);
            } else if ("FAIL".equals(parseResult.getStatus())) {
                resume.setParseStatus(-1);
                resumeMapper.updateById(resume);
            }
        }

        result.put("parseStatus", resume.getParseStatus());
        result.put("statusText", statusText(resume.getParseStatus()));
        if (resume.getParseStatus() == 1) {
            result.put("parsedText", resume.getParsedText());
        }

        return result;
    }

    public Resume getById(Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null) throw new IllegalArgumentException("简历不存在");
        return resume;
    }

    public ResumeAnalysis getAnalysis(Long resumeId) {
        return analysisMapper.selectOne(
            new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
        );
    }

    public List<Resume> getHistory() {
        Long userId = JwtAuthFilter.getCurrentUserId();
        return resumeMapper.selectList(
            new LambdaQueryWrapper<Resume>()
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getCreateTime)
                .last("LIMIT 20")
        );
    }

    public void delete(Long resumeId) {
        Long userId = JwtAuthFilter.getCurrentUserId();
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new IllegalArgumentException("简历不存在");
        }
        resumeMapper.deleteById(resumeId);
        analysisMapper.delete(
            new LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
        );
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf")) return "pdf";
        if (name.endsWith(".doc")) return "doc";
        if (name.endsWith(".docx")) return "docx";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
        if (name.endsWith(".png")) return "png";
        return "unknown";
    }

    private String statusText(int status) {
        return switch (status) {
            case 0 -> "解析中...";
            case 1 -> "解析完成";
            case -1 -> "解析失败";
            default -> "未知";
        };
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/mianmiantong/service/resume/ResumeService.java
git commit -m "feat: add ResumeService for upload and parse management"
```

---

### Task 11: 创建 ResumeController

**Files:**
- Create: `controller/resume/ResumeController.java`
- Create: `dto/resume/ResumeUploadResponse.java`

- [ ] **Step 1: 编写 DTO**

```java
// dto/resume/ResumeAnalyzeRequest.java
package com.mianmiantong.dto.resume;

import lombok.Data;

@Data
public class ResumeAnalyzeRequest {
    private String provider;
}
```

- [ ] **Step 2: 编写 ResumeController**

```java
package com.mianmiantong.controller.resume;

import com.mianmiantong.common.Result;
import com.mianmiantong.service.resume.ResumeService;
import com.mianmiantong.service.resume.ResumeAnalysisService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService analysisService;

    public ResumeController(ResumeService resumeService,
                           ResumeAnalysisService analysisService) {
        this.resumeService = resumeService;
        this.analysisService = analysisService;
    }

    /** 上传简历 */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                           @RequestParam("jobDescription") String jobDescription,
                           @RequestParam(value = "position", required = false) String position) {
        return Result.ok(resumeService.upload(file, jobDescription, position));
    }

    /** 轮询解析状态 */
    @GetMapping("/{resumeId}/status")
    public Result<?> status(@PathVariable Long resumeId) {
        return Result.ok(resumeService.getStatus(resumeId));
    }

    /** AI 分析 (SSE流式) */
    @PostMapping("/{resumeId}/analyze")
    public SseEmitter analyze(@PathVariable Long resumeId) {
        return analysisService.analyzeStream(resumeId);
    }

    /** 获取分析报告 */
    @GetMapping("/{resumeId}/analysis")
    public Result<?> analysis(@PathVariable Long resumeId) {
        return Result.ok(analysisService.getReport(resumeId));
    }

    /** 简历历史 */
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(resumeService.getHistory());
    }

    /** 删除简历 */
    @DeleteMapping("/{resumeId}")
    public Result<?> delete(@PathVariable Long resumeId) {
        resumeService.delete(resumeId);
        return Result.ok();
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/controller/resume/
git add src/main/java/com/mianmiantong/dto/resume/
git commit -m "feat: add ResumeController with upload/status/analyze endpoints"
```

---

## Phase 5: AI 分析服务

### Task 12: 实现 ResumeAnalysisService

**Files:**
- Create: `service/resume/ResumeAnalysisService.java`

- [ ] **Step 1: 编写 ResumeAnalysisService — 流式 AI 分析**

```java
package com.mianmiantong.service.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianmiantong.entity.resume.Resume;
import com.mianmiantong.entity.resume.ResumeAnalysis;
import com.mianmiantong.mapper.resume.ResumeAnalysisMapper;
import com.mianmiantong.mapper.resume.ResumeMapper;
import com.mianmiantong.service.ai.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ResumeAnalysisService {

    private final ResumeMapper resumeMapper;
    private final ResumeAnalysisMapper analysisMapper;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        你是一位资深HR和技术面试官，拥有10年以上的技术招聘经验。
        请对以下简历进行分析，目标岗位为：%s

        ## 分析维度

        1. **结构完整性** (1-10分)
        2. **技术关键词匹配度** (1-10分)
        3. **项目描述质量** (1-10分)
        4. **排版与可读性** (1-10分)
        5. **语言表达** (1-10分)

        ## 输出格式

        请严格按照以下JSON格式输出（不要包含markdown代码块标记）：

        {
          "overallScore": 7,
          "dimensions": [
            {"name": "结构完整性", "score": 7, "comment": "..."}
          ],
          "missingKeywords": ["关键词1", "关键词2"],
          "highlights": [
            {
              "section": "段落标题",
              "before": "优化前文本",
              "after": "优化后文本",
              "reason": "修改理由"
            }
          ],
          "optimizedText": "完整优化后的简历(Markdown格式)",
          "interviewQuestions": ["面试追问1", "面试追问2"],
          "suggestion": "总体提升建议(50-100字)"
        }

        ## 原始简历

        %s

        ## 注意事项
        - 输出JSON必须为一整行，不要换行
        - optimizedText 中的简历优化需保持事实不变，仅优化表达
        - interviewQuestions 需与简历内容真实相关
        """;

    public ResumeAnalysisService(ResumeMapper resumeMapper,
                                  ResumeAnalysisMapper analysisMapper,
                                  AiService aiService) {
        this.resumeMapper = resumeMapper;
        this.analysisMapper = analysisMapper;
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 流式 AI 分析
     */
    public SseEmitter analyzeStream(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || resume.getParseStatus() != 1) {
            throw new IllegalArgumentException("简历不存在或尚未解析完成");
        }

        String systemPrompt = String.format(SYSTEM_PROMPT,
            resume.getJobDescription(), resume.getParsedText());

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", "请开始分析")
        );

        SseEmitter emitter = new SseEmitter(180_000L);
        StringBuilder fullResponse = new StringBuilder();

        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("AI分析超时"));
            } catch (Exception ignored) {}
            emitter.complete();
        });

        CompletableFuture.runAsync(() -> {
            try {
                aiService.streamChat(systemPrompt, messages, null, token -> {
                    fullResponse.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (Exception e) {
                        throw new RuntimeException("SSE发送失败", e);
                    }
                });

                // 解析最终 JSON 并持久化
                String aiResponse = fullResponse.toString();
                String jsonStr = extractJson(aiResponse);

                @SuppressWarnings("unchecked")
                Map<String, Object> report = objectMapper.readValue(jsonStr, Map.class);

                ResumeAnalysis analysis = new ResumeAnalysis();
                analysis.setResumeId(resumeId);
                analysis.setOverallScore(report.get("overallScore") != null
                    ? ((Number) report.get("overallScore")).intValue() : 0);
                analysis.setDimensions(toJson(report.get("dimensions")));
                analysis.setMissingKeywords(toJson(report.get("missingKeywords")));
                analysis.setOptimizedText((String) report.get("optimizedText"));
                analysis.setHighlights(toJson(report.get("highlights")));
                analysis.setInterviewQuestions(toJson(report.get("interviewQuestions")));
                analysis.setSuggestion((String) report.get("suggestion"));

                // upsert: 已存在的报告则更新
                ResumeAnalysis existing = analysisMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                        .eq(ResumeAnalysis::getResumeId, resumeId)
                );
                if (existing != null) {
                    analysis.setId(existing.getId());
                    analysisMapper.updateById(analysis);
                } else {
                    analysisMapper.insert(analysis);
                }

                Map<String, Object> finishData = new LinkedHashMap<>();
                finishData.put("resumeId", resumeId);
                finishData.put("overallScore", analysis.getOverallScore());
                finishData.put("reportId", analysis.getId());

                emitter.send(SseEmitter.event().name("finish")
                    .data(objectMapper.writeValueAsString(finishData)));
                emitter.complete();

            } catch (Exception e) {
                log.error("AI分析异常: resumeId={}", resumeId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data("AI分析失败: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 获取已生成的分析报告
     */
    public Map<String, Object> getReport(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        ResumeAnalysis analysis = analysisMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ResumeAnalysis>()
                .eq(ResumeAnalysis::getResumeId, resumeId)
        );

        if (analysis == null) throw new IllegalArgumentException("分析报告不存在");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumeId", resumeId);
        result.put("overallScore", analysis.getOverallScore());
        result.put("fileName", resume.getFileName());
        result.put("jobDescription", resume.getJobDescription());
        result.put("dimensions", parseJson(analysis.getDimensions()));
        result.put("missingKeywords", parseJson(analysis.getMissingKeywords()));
        result.put("highlights", parseJson(analysis.getHighlights()));
        result.put("optimizedText", analysis.getOptimizedText());
        result.put("interviewQuestions", parseJson(analysis.getInterviewQuestions()));
        result.put("suggestion", analysis.getSuggestion());
        return result;
    }

    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}") + 1;
        if (start >= 0 && end > start) {
            return response.substring(start, end);
        }
        return response;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "null"; }
    }

    private Object parseJson(String json) {
        if (json == null) return null;
        try { return objectMapper.readValue(json, Object.class); }
        catch (JsonProcessingException e) { return json; }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 3: 启动验证接口可访问**

```bash
mvn spring-boot:run &
sleep 10
curl -s http://localhost:8080/doc.html | grep -o "resume" | head -1
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/service/resume/
git commit -m "feat: add ResumeAnalysisService with SSE streaming AI analysis"
```

---

## Phase 6: 前端页面

### Task 13: 注册路由分包

**Files:**
- Modify: `AI-Interview/pages.json`

- [ ] **Step 1: 在 subPackages 数组中添加简历分包**

在 `pages.json` 的 `subPackages` 数组末尾添加:

```json
{
    "root": "pages/resume",
    "pages": [
        {
            "path": "upload",
            "style": {
                "navigationBarTitleText": "简历优化"
            }
        },
        {
            "path": "report",
            "style": {
                "navigationBarTitleText": "分析报告"
            }
        }
    ]
}
```

- [ ] **Step 2: 创建页面目录**

```bash
mkdir -p AI-Interview/pages/resume
```

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/pages.json
git commit -m "feat: register resume sub-package routes"
```

---

### Task 14: 实现 upload.vue — 上传页

**Files:**
- Create: `AI-Interview/pages/resume/upload.vue`

- [ ] **Step 1: 编写 upload.vue**

```vue
<template>
  <view class="upload-page">
    <!-- 标题区 -->
    <view class="header">
      <text class="h-title">简历优化</text>
      <text class="h-desc">上传简历，AI 帮你分析优化</text>
    </view>

    <!-- 文件选择区 -->
    <view class="section">
      <view class="file-picker" @click="chooseFile">
        <view class="fp-icon" v-if="!fileInfo">📄</view>
        <view class="fp-icon" v-else>✅</view>
        <text class="fp-text" v-if="!fileInfo">点击选择简历文件</text>
        <text class="fp-text" v-else>{{ fileInfo.name }}</text>
        <text class="fp-hint" v-if="!fileInfo">支持 PDF、Word、图片</text>
        <text class="fp-hint" v-else>{{ formatSize(fileInfo.size) }}</text>
      </view>
    </view>

    <!-- JD 输入区 -->
    <view class="section">
      <text class="s-label">目标岗位描述</text>
      <textarea class="jd-input" v-model="jobDescription"
        placeholder="粘贴目标岗位的 JD 描述，如：&#10;负责公司核心业务系统的后端开发，要求：&#10;1. 精通Java，熟悉Spring Boot&#10;2. 熟悉MySQL、Redis&#10;3. 有分布式系统经验..."
        :maxlength="-1" auto-height />
    </view>

    <!-- 岗位名称 -->
    <view class="section">
      <text class="s-label">岗位名称（选填）</text>
      <input class="pos-input" v-model="position" placeholder="如：Java后端开发工程师" />
    </view>

    <!-- 提交按钮 -->
    <view class="btn-wrap">
      <button class="btn-upload" :disabled="uploading || !canSubmit" @click="doUpload">
        {{ uploading ? '上传中...' : '上传并解析' }}
      </button>
    </view>

    <!-- 解析进度 -->
    <view class="progress-section" v-if="uploading">
      <view class="progress-bar">
        <view class="progress-fill" :class="progressClass" :style="{ width: progressWidth }" />
      </view>
      <text class="progress-text">{{ progressText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { get, post } from '@/utils/request';

interface FileInfo { name: string; size: number; path: string; }

const fileInfo = ref<FileInfo | null>(null);
const jobDescription = ref('');
const position = ref('');
const uploading = ref(false);
const progressWidth = ref('0%');
const progressClass = ref('');
const progressText = ref('');

const canSubmit = computed(() => fileInfo.value && jobDescription.value.trim());

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

function chooseFile() {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png'],
    success: (res) => {
      fileInfo.value = {
        name: res.tempFiles[0].name,
        size: res.tempFiles[0].size,
        path: res.tempFiles[0].path,
      };
    },
  });
}

async function doUpload() {
  if (!canSubmit.value || uploading.value) return;
  uploading.value = true;
  progressWidth.value = '20%';
  progressText.value = '正在上传...';

  try {
    // Step 1: 上传文件
    const res = await uni.uploadFile({
      url: 'http://localhost:8080/api/resume/upload',
      filePath: fileInfo.value!.path,
      name: 'file',
      formData: {
        jobDescription: jobDescription.value.trim(),
        position: position.value.trim(),
      },
      header: {
        Authorization: 'Bearer ' + uni.getStorageSync('mianmiantong_token'),
      },
    });

    const data = JSON.parse(res.data);
    if (data.code !== 200) throw new Error(data.message);

    const { resumeId } = data.data;
    progressWidth.value = '40%';
    progressText.value = '正在解析简历...';

    // Step 2: 轮询解析状态
    await pollStatus(resumeId);

  } catch (e: any) {
    uni.showToast({ title: e.message || '上传失败', icon: 'error' });
    uploading.value = false;
  }
}

async function pollStatus(resumeId: number) {
  let attempts = 0;
  const maxAttempts = 30;
  progressClass.value = 'animating';

  while (attempts < maxAttempts) {
    await sleep(2000);
    attempts++;

    const pct = 40 + Math.floor(attempts / maxAttempts * 40);
    progressWidth.value = pct + '%';

    try {
      const r = await get<{ parseStatus: number; statusText: string; parsedText?: string }>(
        `/api/resume/${resumeId}/status`
      );

      if (r.data.parseStatus === 1) {
        progressWidth.value = '100%';
        progressText.value = '解析完成，正在跳转...';
        await sleep(500);
        uni.navigateTo({ url: `/pages/resume/report?resumeId=${resumeId}` });
        return;
      }
      if (r.data.parseStatus === -1) {
        throw new Error('解析失败，请检查文件格式');
      }
      progressText.value = r.data.statusText;
    } catch (e: any) {
      if (e.message.includes('解析失败')) throw e;
    }
  }
  throw new Error('解析超时，请重试');
}

function sleep(ms: number) { return new Promise(resolve => setTimeout(resolve, ms)); }
</script>

<style lang="scss" scoped>
.upload-page { min-height: 100vh; background: #f0f4ff; padding: 30rpx; }
.header { text-align: center; padding: 40rpx 0; }
.h-title { font-size: 40rpx; font-weight: 800; color: #0f172a; display: block; }
.h-desc { font-size: 26rpx; color: #94a3b8; margin-top: 10rpx; display: block; }

.section { margin-bottom: 30rpx; }
.s-label { font-size: 28rpx; font-weight: 600; color: #1e293b; display: block; margin-bottom: 14rpx; }

.file-picker {
  background: #fff; border: 2rpx dashed #cbd5e1; border-radius: 20rpx;
  padding: 60rpx 40rpx; text-align: center;
  transition: all 0.2s;
}
.file-picker:active { border-color: #2b6ff2; background: #f8faff; }
.fp-icon { font-size: 64rpx; margin-bottom: 16rpx; }
.fp-text { font-size: 28rpx; color: #1e293b; font-weight: 500; display: block; }
.fp-hint { font-size: 24rpx; color: #94a3b8; margin-top: 6rpx; display: block; }

.jd-input {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  font-size: 26rpx; line-height: 1.6; min-height: 200rpx;
  width: 100%; box-sizing: border-box;
}
.pos-input {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  font-size: 26rpx; width: 100%; box-sizing: border-box;
}

.btn-wrap { padding: 20rpx 0; }
.btn-upload {
  width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff);
  color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none;
}
.btn-upload[disabled] { background: #cbd5e1; color: #94a3b8; }

.progress-section { margin-top: 30rpx; }
.progress-bar { height: 8rpx; background: #e2e8f0; border-radius: 4rpx; overflow: hidden; }
.progress-fill { height: 100%; background: #2b6ff2; border-radius: 4rpx; transition: width 0.5s; }
.progress-fill.animating { background: linear-gradient(90deg, #2b6ff2, #6366f1, #2b6ff2); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.progress-text { font-size: 24rpx; color: #64748b; margin-top: 12rpx; display: block; text-align: center; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/pages/resume/upload.vue
git commit -m "feat: add resume upload page with async polling"
```

---

### Task 15: 实现 report.vue — 分析报告页

**Files:**
- Create: `AI-Interview/pages/resume/report.vue`

- [ ] **Step 1: 编写 report.vue**

```vue
<template>
  <view class="report-page">
    <!-- 加载中 -->
    <view class="loading-screen" v-if="loading">
      <view class="loading-spinner" />
      <text class="loading-text">{{ loadingText }}</text>
    </view>

    <!-- 报告内容 -->
    <template v-if="!loading && report">
      <!-- 评分 -->
      <view class="score-hero">
        <view class="score-ring" :class="score >= 7 ? 'great' : score >= 4 ? 'ok' : 'low'">
          <text class="score-num">{{ score }}</text>
          <text class="score-unit">/10</text>
        </view>
        <text class="score-label">简历综合评分</text>
        <text class="score-file">{{ report.fileName }}</text>
      </view>

      <!-- 维度评分 -->
      <view class="card" v-if="report.dimensions">
        <text class="card-label">能力维度</text>
        <view class="dim-item" v-for="d in report.dimensions" :key="d.name">
          <view class="dim-head">
            <text class="dim-name">{{ d.name }}</text>
            <text class="dim-score">{{ d.score }}/10</text>
          </view>
          <view class="dim-bar-bg"><view class="dim-bar-fill" :style="{ width: (d.score * 10) + '%' }" /></view>
          <text class="dim-comment" v-if="d.comment">{{ d.comment }}</text>
        </view>
      </view>

      <!-- 缺失关键词 -->
      <view class="card" v-if="report.missingKeywords && report.missingKeywords.length">
        <text class="card-label">缺失关键词（对标 JD）</text>
        <view class="keywords">
          <text class="kw-tag" v-for="kw in report.missingKeywords" :key="kw">{{ kw }}</text>
        </view>
      </view>

      <!-- 优化建议 -->
      <view class="card" v-if="report.suggestion">
        <text class="card-label">总体建议</text>
        <text class="card-text">{{ report.suggestion }}</text>
      </view>

      <!-- 逐段优化对比 -->
      <view class="card" v-if="report.highlights && report.highlights.length">
        <text class="card-label">逐段优化对比</text>
        <view class="highlight-item" v-for="(h, i) in report.highlights" :key="i">
          <text class="hl-section">{{ h.section }}</text>
          <view class="hl-before"><text class="hl-tag">原文</text><text class="hl-text">{{ h.before }}</text></view>
          <view class="hl-after"><text class="hl-tag opt">优化</text><text class="hl-text">{{ h.after }}</text></view>
          <text class="hl-reason">{{ h.reason }}</text>
        </view>
      </view>

      <!-- 优化后完整简历 -->
      <view class="card" v-if="report.optimizedText">
        <view class="card-label-row">
          <text class="card-label">优化后简历</text>
          <button class="btn-copy" @click="copyText(report.optimizedText)">复制</button>
        </view>
        <view class="optimized-resume">
          <text class="opt-text">{{ report.optimizedText }}</text>
        </view>
      </view>

      <!-- 面试追问 -->
      <view class="card" v-if="report.interviewQuestions && report.interviewQuestions.length">
        <text class="card-label">面试可能追问</text>
        <view class="iq-item" v-for="(q, i) in report.interviewQuestions" :key="i">
          <text class="iq-num">{{ i + 1 }}</text>
          <text class="iq-text">{{ q }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="actions">
        <button class="btn-primary" @click="goInterview">应用到面试</button>
        <button class="btn-secondary" @click="goHome">返回首页</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post, streamRequest } from '@/utils/request';

interface Dim { name: string; score: number; comment: string; }
interface Highlight { section: string; before: string; after: string; reason: string; }
interface Report {
  resumeId: number; overallScore: number; fileName: string; jobDescription: string;
  dimensions: Dim[]; missingKeywords: string[]; highlights: Highlight[];
  optimizedText: string; interviewQuestions: string[]; suggestion: string;
}

const loading = ref(true);
const loadingText = ref('AI 正在分析简历...');
const report = ref<Report | null>(null);
const score = ref(0);

onLoad(async (opts) => {
  const resumeId = Number(opts?.resumeId);

  // 优先加载已有报告
  try {
    const r = await get<Report>(`/api/resume/${resumeId}/analysis`);
    if (r.data && r.data.overallScore != null) {
      report.value = r.data;
      score.value = r.data.overallScore;
      loading.value = false;
      return;
    }
  } catch {}

  // 无报告则触发 AI 分析 (SSE流式)
  const tokenList: string[] = [];
  streamRequest(
    `/api/resume/${resumeId}/analyze`,
    {},
    {
      onToken: (token) => {
        tokenList.push(token);
        loadingText.value = tokenList.join('').slice(-50);
      },
      onFinish: async (data) => {
        loadingText.value = '分析完成，加载报告...';
        const r = await get<Report>(`/api/resume/${resumeId}/analysis`);
        report.value = r.data;
        score.value = r.data.overallScore || 0;
        loading.value = false;
      },
      onError: (err) => {
        uni.showToast({ title: err.message, icon: 'error' });
        loading.value = false;
      },
    }
  );
});

function copyText(text: string) {
  uni.setClipboardData({ data: text, success: () => uni.showToast({ title: '已复制' }) });
}

function goInterview() {
  if (report.value) {
    uni.setStorageSync('resumeForInterview', JSON.stringify({
      resumeId: report.value.resumeId,
      parsedText: report.value.optimizedText,
    }));
  }
  uni.navigateTo({ url: '/pages/interview/chat?fromResume=1' });
}

function goHome() { uni.switchTab({ url: '/pages/index/index' }); }
</script>

<style lang="scss" scoped>
.report-page { min-height: 100vh; background: #f0f4ff; padding-bottom: 40rpx; }
.loading-screen { display: flex; flex-direction: column; align-items: center; padding-top: 300rpx; }
.loading-spinner { width: 80rpx; height: 80rpx; border: 6rpx solid #e2e8f0; border-top-color: #2b6ff2; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { font-size: 26rpx; color: #64748b; margin-top: 30rpx; }

.score-hero { display: flex; flex-direction: column; align-items: center; padding: 60rpx 0 50rpx; background: linear-gradient(135deg, #1a3a6b, #2b6ff2, #4f8dff); }
.score-ring { width: 180rpx; height: 180rpx; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 6rpx solid rgba(255,255,255,0.3); }
.score-num { font-size: 72rpx; font-weight: 900; color: #fff; }
.score-unit { font-size: 24rpx; color: rgba(255,255,255,0.7); }
.score-label { font-size: 28rpx; color: rgba(255,255,255,0.8); margin-top: 16rpx; }
.score-file { font-size: 22rpx; color: rgba(255,255,255,0.5); margin-top: 6rpx; }

.card { background: #fff; margin: 20rpx 24rpx; padding: 28rpx; border-radius: 20rpx; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03); }
.card-label { font-size: 28rpx; font-weight: 700; color: #0f172a; display: block; margin-bottom: 16rpx; }
.card-label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.card-text { font-size: 26rpx; color: #64748b; line-height: 1.8; display: block; }
.btn-copy { font-size: 24rpx; color: #2b6ff2; background: #f0f4ff; padding: 8rpx 24rpx; border-radius: 20rpx; border: none; }

.dim-item { padding: 16rpx 0; }
.dim-item + .dim-item { border-top: 1rpx solid #f1f5f9; }
.dim-head { display: flex; justify-content: space-between; margin-bottom: 10rpx; }
.dim-name { font-size: 26rpx; font-weight: 600; color: #1e293b; }
.dim-score { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.dim-bar-bg { height: 6rpx; background: #e2e8f0; border-radius: 3rpx; overflow: hidden; }
.dim-bar-fill { height: 100%; background: linear-gradient(90deg, #2b6ff2, #6366f1); border-radius: 3rpx; transition: width 0.6s; }
.dim-comment { font-size: 22rpx; color: #94a3b8; margin-top: 6rpx; display: block; }

.keywords { display: flex; flex-wrap: wrap; gap: 12rpx; }
.kw-tag { font-size: 22rpx; background: #fef2f2; color: #ef4444; padding: 6rpx 16rpx; border-radius: 8rpx; }

.highlight-item { padding: 20rpx 0; }
.highlight-item + .highlight-item { border-top: 1rpx solid #f1f5f9; }
.hl-section { font-size: 24rpx; font-weight: 700; color: #2b6ff2; display: block; margin-bottom: 12rpx; }
.hl-before, .hl-after { display: flex; gap: 12rpx; margin-bottom: 8rpx; }
.hl-tag { font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 6rpx; background: #fef2f2; color: #ef4444; font-weight: 600; }
.hl-tag.opt { background: #ecfdf5; color: #10b981; }
.hl-text { font-size: 24rpx; color: #64748b; flex: 1; line-height: 1.6; }
.hl-reason { font-size: 22rpx; color: #94a3b8; padding-left: 48rpx; display: block; }

.optimized-resume { background: #f8fafc; border-radius: 12rpx; padding: 24rpx; }
.opt-text { font-size: 26rpx; color: #1e293b; line-height: 1.8; white-space: pre-wrap; }

.iq-item { display: flex; gap: 14rpx; padding: 14rpx 0; }
.iq-item + .iq-item { border-top: 1rpx solid #f1f5f9; }
.iq-num { width: 40rpx; height: 40rpx; background: #f0f4ff; color: #2b6ff2; font-size: 22rpx; font-weight: 700; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.iq-text { font-size: 26rpx; color: #1e293b; line-height: 1.6; }

.actions { padding: 30rpx 24rpx; display: flex; flex-direction: column; gap: 16rpx; }
.btn-primary { width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; }
.btn-secondary { width: 100%; height: 96rpx; background: #f1f5f9; color: #64748b; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/pages/resume/report.vue
git commit -m "feat: add resume analysis report page"
```

---

### Task 16: 首页新增简历入口

**Files:**
- Modify: `AI-Interview/pages/index/index.vue`

- [ ] **Step 1: 在 actions 区域增加简历卡片**

在 `index.vue` 的 `<view class="actions">` 中，错题本卡片之后添加:

```vue
<view class="action resume-opt" @click="goResume">
  <view class="action-icon-wrap">
    <text class="action-icon">📋</text>
  </view>
  <text class="action-title">简历优化</text>
  <text class="action-desc">AI智能优化</text>
</view>
```

在 `<script>` 中添加导航方法:
```typescript
function goResume() { uni.navigateTo({ url: '/pages/resume/upload' }); }
```

在 `<style>` 中添加样式:
```scss
.resume-opt .action-icon-wrap { background: linear-gradient(135deg, #f0fdf4, #dcfce7); }
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/pages/index/index.vue
git commit -m "feat: add resume optimization entry on homepage"
```

---

## Phase 7: 面试联动

### Task 17: chat.vue 简历入口

**Files:**
- Modify: `AI-Interview/pages/interview/chat.vue`

- [ ] **Step 1: 在 position-screen 中添加简历选择区域**

在 `position-screen` 的 `pos-grid` 之前插入:

```vue
<view class="resume-section" v-if="resumeList.length > 0">
  <text class="rs-label">📋 已有简历分析报告，可引入面试</text>
  <picker mode="selector" :range="resumeNames" @change="onResumePick">
    <view class="rs-picker">{{ selectedResumeName || '选择简历（可选）' }}</view>
  </picker>
</view>
```

- [ ] **Step 2: 在 script 中添加逻辑**

```typescript
const resumeList = ref<Array<{id: number; position: string}>>([]);
const resumeNames = computed(() => resumeList.value.map(r => r.position || `简历 #${r.id}`));
const selectedResumeId = ref<number | null>(null);
const selectedResumeName = ref('');

onMounted(async () => {
  try {
    const r = await get<Array<{id: number; position: string}>>('/api/resume/list');
    resumeList.value = r.data || [];
  } catch {}
});

function onResumePick(e: { detail: { value: number } }) {
  selectedResumeId.value = resumeList.value[e.detail.value]?.id;
  selectedResumeName.value = resumeNames.value[e.detail.value];
}
```

- [ ] **Step 3: startInterview 携带 resumeId**

```typescript
async function startInterview(pos: string) {
  try {
    uni.showLoading({ title: '思考中...' });
    const body: Record<string, unknown> = { position: pos };
    if (selectedResumeId.value) {
      body.resumeId = selectedResumeId.value;
    }
    const r = await post<{ sessionId: number; question: string }>('/api/interview/start', body);
    // ... rest unchanged
  } catch { /* ... */ }
}
```

- [ ] **Step 4: Commit**

```bash
git add AI-Interview/pages/interview/chat.vue
git commit -m "feat: add resume integration in AI interview"
```

---

### Task 18: InterviewService 支持简历上下文

**Files:**
- Modify: `service/interview/InterviewService.java`
- Modify: `dto/interview/InterviewStartRequest.java`

- [ ] **Step 1: InterviewStartRequest 增加 resumeId 字段**

```java
package com.mianmiantong.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewStartRequest {
    @NotBlank
    private String position;
    private Long resumeId;  // 新增
}
```

- [ ] **Step 2: InterviewService.start() 增加简历上下文注入**

在 `start()` 方法中，`initMessages` 构建之前添加:

```java
// 如果携带了简历，获取简历文本并注入 System Prompt
String resumeContext = "";
if (request.getResumeId() != null) {
    Resume resume = resumeMapper.selectById(request.getResumeId());
    if (resume != null && resume.getParseStatus() == 1) {
        resumeContext = String.format("""

            ## 候选人简历背景

            %s
            """, resume.getParsedText());
    }
}

String systemPrompt = String.format(SYSTEM_PROMPT, position) + resumeContext;
```

需要在 InterviewService 构造函数注入 `ResumeMapper`:
```java
private final ResumeMapper resumeMapper;

public InterviewService(InterviewSessionMapper sessionMapper, AiService aiService,
                        UserAiConfigService userAiConfigService, ResumeMapper resumeMapper) {
    // ...
    this.resumeMapper = resumeMapper;
}
```

- [ ] **Step 3: 编译验证**

```bash
cd mianmiantong-server && mvn compile -q
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/mianmiantong/
git commit -m "feat: inject resume context into interview system prompt"
```

---

## Phase 8: 集成验证

### Task 19: 端到端验证

- [ ] **Step 1: 启动后端**

```bash
cd mianmiantong-server && mvn spring-boot:run
```

- [ ] **Step 2: 验证 API 可用性**

```bash
# 1. 登录获取 token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"code":"test123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])"

# 2. 验证简历列表(空)
curl -s http://localhost:8080/api/resume/list \
  -H "Authorization: Bearer <token>" | python3 -m json.tool

# 3. 验证文档智能
# (需要真实文件，可手动通过 Swagger 测试)
```

- [ ] **Step 3: 前端编译**

```bash
# 在 HBuilderX 中编译到微信小程序
# 验证 pages/resume/upload 和 pages/resume/report 页面可访问
```

- [ ] **Step 4: 手动测试完整流程**

1. 首页点击「简历优化」→ 进入 upload 页
2. 选择 PDF 文件 + 填写 JD → 上传
3. 观察轮询进度 → 解析完成 → 跳转 report
4. report 中查看评分/维度/优化对比
5. 点击「应用到面试」→ 进入 chat → 面试官基于简历提问

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: end-to-end verification passed"
```
