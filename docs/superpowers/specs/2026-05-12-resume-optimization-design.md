# 简历优化功能 — 设计规格说明

> 日期: 2026-05-12 | 状态: 待审批 | 关联需求: 简历上传 + AI 分析 + 面试联动

---

## 1. 需求概述

用户在小程序内上传简历文件（PDF/Word/图片），系统自动解析为结构化文本，结合用户输入的目标岗位描述（JD），调用大模型进行多维分析，输出综合评分、修改建议、优化后简历文本及优化点高亮对比。

---

## 2. 功能流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 上传简历  │ →  │ 文档解析  │ →  │ AI 分析  │ →  │ 报告展示  │ →  │ 面试引用  │
│          │    │ (异步轮询) │    │ (流式SSE) │    │          │    │          │
│ 选择文件  │    │ Aliyun   │    │ DeepSeek │    │ 综合评分  │    │ 面试官可  │
│ 填写JD   │    │ DocumentAI│   │ /Qwen    │    │ 优化建议  │    │ 基于简历  │
│          │    │          │    │          │    │ 高亮对比  │    │ 提问     │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘

状态流转: uploaded → parsing → parsed → analyzing → completed
                                            ↘ parse_failed
                                            ↘ analysis_failed
```

### 2.1 独立页面路径

- 首页快捷入口 → `pages/resume/upload`（上传页）
- 上传完成后 → `pages/resume/report`（报告页）
- AI 面试中 → `pages/interview/chat` 岗位选择区增加「上传简历」入口

### 2.2 与 AI 面试联动

面试 chat 页面岗位选择区新增「上传简历」入口：
1. 若用户已上传过简历，显示历史简历列表供选择
2. 选择后，简历解析文本作为 System Prompt 的附加上下文，面试官可针对简历内容提问
3. 分析报告中生成的 `interview_questions` 自动加入面试题库

---

## 3. 后端模块设计

### 3.1 新增目录（domain 分包）

```
com/mianmiantong/
├── common/                         # 共享工具 (不变)
│   ├── Result.java
│   └── JwtUtil.java
├── config/                         # 配置 (不变)
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   └── GlobalExceptionHandler.java
├── controller/
│   ├── auth/AuthController.java
│   ├── question/QuestionController.java
│   ├── exam/ExamController.java
│   ├── interview/InterviewController.java
│   ├── wrongbook/WrongQuestionController.java
│   ├── user/UserController.java
│   ├── answer/AnswerController.java
│   └── resume/ResumeController.java       # 新增
├── dto/
│   ├── auth/LoginRequest.java, LoginResponse.java
│   ├── question/PageQuery.java
│   ├── exam/ExamSubmitRequest.java
│   ├── interview/InterviewStartRequest.java, InterviewAnswerRequest.java
│   ├── user/UserAiConfigRequest.java
│   ├── answer/AnswerSubmitRequest.java
│   └── resume/                              # 新增
│       ├── ResumeUploadResponse.java
│       ├── ResumeStatusResponse.java
│       ├── ResumeAnalyzeRequest.java
│       └── ResumeReportResponse.java
├── entity/
│   ├── user/User.java, UserAiConfig.java, UserFavorite.java
│   ├── question/Question.java, QuestionCategory.java
│   ├── exam/Exam.java, ExamQuestion.java, AnswerRecord.java
│   ├── interview/InterviewSession.java
│   ├── wrongbook/WrongQuestion.java
│   └── resume/                              # 新增
│       ├── Resume.java
│       └── ResumeAnalysis.java
├── mapper/                          # 与 entity 包一一对应
│   ├── user/
│   ├── question/
│   ├── exam/
│   ├── interview/
│   ├── wrongbook/
│   └── resume/                              # 新增
│       ├── ResumeMapper.java
│       └── ResumeAnalysisMapper.java
└── service/
    ├── auth/AuthService.java
    ├── question/QuestionService.java
    ├── exam/ExamService.java
    ├── interview/InterviewService.java
    ├── wrongbook/WrongQuestionService.java
    ├── user/UserAiConfigService.java
    ├── answer/AnswerService.java
    ├── ai/                                   # 已有
    │   ├── AiService.java
    │   ├── DeepSeekAiService.java
    │   └── QwenAiService.java
    ├── document/                             # 新增 - 文档解析
    │   ├── DocumentAiService.java            # 阿里云文档智能接口
    │   └── DocumentParseResult.java          # 解析结果 DTO
    └── resume/                               # 新增 - 简历业务
        ├── ResumeService.java                # 上传 + 解析任务管理
        └── ResumeAnalysisService.java        # AI 分析编排
```

> **重构说明**：现有 controller/dto/entity/mapper/service 下的文件将按上表迁移到对应子包。MyBatis-Plus 的 `@MapperScan("com.mianmiantong.mapper")` 能自动扫描子包，无需修改配置。

---

## 4. API 接口协议

### 4.1 上传简历 → 提交解析

```
POST /api/resume/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 简历文件 (pdf/doc/docx/jpg/png) |
| jobDescription | String | 是 | 目标岗位 JD 描述 |
| position | String | 否 | 目标岗位名称 |

**Response (200):**

```json
{
  "code": 200,
  "data": {
    "resumeId": 1,
    "taskId": "docmind-20260512-xxxx",
    "parseStatus": 0,
    "fileName": "简历.pdf"
  }
}
```

### 4.2 轮询解析状态

```
GET /api/resume/{resumeId}/status
Authorization: Bearer <token>
```

**Response (解析中):**

```json
{
  "code": 200,
  "data": {
    "resumeId": 1,
    "parseStatus": 0,
    "statusText": "解析中..."
  }
}
```

**Response (解析完成):**

```json
{
  "code": 200,
  "data": {
    "resumeId": 1,
    "parseStatus": 1,
    "statusText": "解析完成",
    "parsedText": "姓名：张三\n教育经历：清华大学 计算机科学 2021-2025\n..."
  }
}
```

**Response (解析失败):**

```json
{
  "code": 200,
  "data": {
    "resumeId": 1,
    "parseStatus": -1,
    "statusText": "解析失败：文件格式不支持"
  }
}
```

> `parseStatus`: 0=解析中, 1=完成, -1=失败

### 4.3 开始 AI 分析（SSE 流式）

```
POST /api/resume/{resumeId}/analyze
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "provider": "deepseek"
}
```

**SSE 事件流:**

```
event: token
data: 综合评分：7/10

event: token
data: ---

event: token
data: **结构完整性 (7/10)**：个人信息、教育背景完整...

event: progress
data: {"stage":"dimensions","current":2,"total":5}

event: highlight
data: {"before":"参与项目开发","after":"主导xx系统架构设计，支撑日均100万请求","reason":"使用STAR原则，量化成果"}

event: finish
data: {"resumeId":1,"overallScore":7,"reportId":1}
```

SSE 事件类型：

| event | data 内容 | 说明 |
|-------|-----------|------|
| `token` | 文本片段 | 分析内容流式输出 |
| `progress` | `{"stage":"dimensions","current":2,"total":5}` | 分析阶段进度 |
| `highlight` | `{"before":"...","after":"...","reason":"..."}` | 逐段优化对比 |
| `finish` | `{"resumeId":1,"overallScore":7,"reportId":1}` | 分析完成 |

### 4.4 获取分析报告

```
GET /api/resume/{resumeId}/analysis
Authorization: Bearer <token>
```

**Response:**

```json
{
  "code": 200,
  "data": {
    "resumeId": 1,
    "overallScore": 7,
    "fileName": "简历.pdf",
    "jobDescription": "Java后端开发工程师...",
    "dimensions": [
      {"name": "结构完整性", "score": 7, "comment": "模块齐全，缺少个人总结"},
      {"name": "技术关键词", "score": 6, "comment": "缺少Spring Cloud、Redis等关键词"},
      {"name": "项目描述质量", "score": 6, "comment": "STAR原则应用不足，缺少量化数据"},
      {"name": "排版与可读性", "score": 8, "comment": "层级清晰，重点突出"},
      {"name": "语言表达", "score": 7, "comment": "整体流畅，个别句子过长"}
    ],
    "missingKeywords": ["Spring Cloud", "Redis", "Docker", "消息队列"],
    "highlights": [
      {
        "section": "项目经验",
        "before": "参与电商平台后端开发",
        "after": "主导电商平台订单系统架构设计，使用Spring Boot + MyBatis-Plus，日均处理订单10万+，响应时间<50ms",
        "reason": "使用STAR原则，增加技术栈细节和量化成果"
      }
    ],
    "optimizedText": "# 张三\n## 教育经历\n...",
    "interviewQuestions": [
      "你在订单系统中如何处理高并发场景？",
      "Redis在你的项目中是如何使用的？"
    ],
    "suggestion": "建议重点补充项目量化成果，突出中间件使用经验"
  }
}
```

### 4.5 简历历史列表

```
GET /api/resume/list
Authorization: Bearer <token>
```

### 4.6 删除简历

```
DELETE /api/resume/{resumeId}
Authorization: Bearer <token>
```

---

## 5. 数据库设计

### 5.1 resume — 简历表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 简历 ID |
| user_id | BIGINT | NOT NULL, INDEX | 用户 ID |
| file_name | VARCHAR(200) | NOT NULL | 原始文件名 |
| file_type | VARCHAR(20) | NOT NULL | pdf/docx/jpg/png |
| file_size | BIGINT | — | 文件大小(字节) |
| job_description | TEXT | NOT NULL | 目标岗位 JD |
| position | VARCHAR(50) | — | 目标岗位名称 |
| parsed_text | MEDIUMTEXT | — | 解析后的结构化文本 |
| parse_status | TINYINT | DEFAULT 0 | 0=解析中 1=完成 -1=失败 |
| doc_task_id | VARCHAR(100) | — | 阿里云任务 ID |
| create_time | DATETIME | DEFAULT NOW() | 上传时间 |

迁移文件: `V4__resume.sql`

### 5.2 resume_analysis — 分析报告表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 报告 ID |
| resume_id | BIGINT | NOT NULL, UNIQUE | 简历 ID |
| overall_score | INT | — | 综合评分 1-10 |
| dimensions | JSON | — | 五维度评分 `[{name,score,comment}]` |
| missing_keywords | JSON | — | 缺失关键词 `["Redis","Docker"]` |
| optimized_text | MEDIUMTEXT | — | 优化后完整简历(Markdown) |
| highlights | JSON | — | 优化对比 `[{section,before,after,reason}]` |
| interview_questions | JSON | — | 面试追问 `["问题1","问题2"]` |
| suggestion | TEXT | — | 总体提升建议 |
| create_time | DATETIME | DEFAULT NOW() | 分析时间 |

迁移文件: `V5__resume_analysis.sql`

---

## 6. AI 分析 System Prompt 设计

```
你是一位资深HR和技术面试官，拥有10年以上的技术招聘经验。
请对以下简历进行分析，目标岗位为：{jobDescription}

## 分析维度

1. **结构完整性** (1-10分)：简历是否包含个人信息、教育背景、
   技能栈、项目经验、实习/工作经历等完整模块

2. **技术关键词匹配度** (1-10分)：简历中的技术关键词与目标JD
   的匹配程度，列出缺失的关键词

3. **项目描述质量** (1-10分)：是否遵循STAR原则（情境-任务-
   行动-结果），是否有量化成果

4. **排版与可读性** (1-10分)：段落长度、层级结构、重点是否突出

5. **语言表达** (1-10分)：用词专业度、是否有冗余表达、语法错误

## 输出格式

请严格按照以下JSON格式输出（不要包含markdown代码块标记）：

{
  "overallScore": 7,
  "dimensions": [
    {"name": "结构完整性", "score": 7, "comment": "..."},
    ...
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

{parsedText}

## 注意事项

- 输出JSON必须为一整行，不要换行
- optimizedText 中的简历优化需保持事实不变，仅优化表达
- interviewQuestions 需与简历内容真实相关
- 对中文简历的表述习惯保持敏感
```

---

## 7. 阿里云文档智能集成

### 7.1 配置项

```yaml
aliyun:
  access-key-id: ${ALIBABA_CLOUD_ACCESS_KEY_ID}
  access-key-secret: ${ALIBABA_CLOUD_ACCESS_KEY_SECRET}
  docmind:
    endpoint: docmind-api.cn-hangzhou.aliyuncs.com
```

### 7.2 调用流程

```
ResumeService.upload(file, jd)
  └→ 上传文件到 OSS 或转为 Base64
  └→ DocumentAiService.submitParse(file)
      └→ POST docmind-api → {taskId}
  └→ 写入 resume 表 (parse_status=0, doc_task_id=taskId)

ResumeService.pollStatus(resumeId)
  └→ DocumentAiService.getResult(taskId)
      └→ GET docmind-api?taskId=xxx → {status, parsedText}
  └→ 更新 resume 表 (parse_status, parsed_text)

ResumeAnalysisService.analyze(resumeId, provider)
  └→ 读取 resume.parsed_text + job_description
  └→ 构建 System Prompt
  └→ AiService.streamChat(prompt, messages, onToken)
  └→ 流式解析 SSE → 逐 token 推送前端
  └→ 完成时解析最终 JSON → 写入 resume_analysis 表
```

---

## 8. 前端新增页面

### 8.1 路由注册

```json
// pages.json subPackages 新增
{
  "root": "pages/resume",
  "pages": [
    { "path": "upload", "style": { "navigationBarTitleText": "简历优化" } },
    { "path": "report", "style": { "navigationBarTitleText": "分析报告" } }
  ]
}
```

### 8.2 upload.vue — 上传页

- 文件选择器（支持 pdf/doc/docx/jpg/png）
- JD 输入框（多行文本域 + 岗位名称选择）
- 上传按钮 → 异步轮询进度 → 解析完成自动跳转 report

### 8.3 report.vue — 报告页

- 评分环形图（1-10）
- 五维度柱状条（复用 interview/report.vue 组件样式）
- 缺失关键词标签云
- 逐段优化对比（before/after 卡片，高亮差异）
- 完整优化简历（rich-text 渲染 Markdown）
- 面试追问列表
- "应用到面试"按钮 → 跳转 interview/chat 并携带 resumeId

### 8.4 interview/chat.vue 修改

岗位选择屏下方增加：
- 「已上传简历」区域（显示最近 3 条）
- 选择简历后，`resumeId` 作为 `startInterview` 的附加参数
- 后端将简历文本注入 System Prompt 的「候选人背景」部分

---

## 9. 迭代计划

| 阶段 | 内容 | 预估工时 |
|------|------|---------|
| **Phase 1** | 文件目录重构（现有文件迁移到 domain 子包） | 0.5d |
| **Phase 2** | 数据库迁移 V4/V5 + 实体/Mapper | 0.5d |
| **Phase 3** | DocumentAiService（阿里云文档智能集成） | 1d |
| **Phase 4** | ResumeService + ResumeController（上传/轮询/CRUD） | 1d |
| **Phase 5** | ResumeAnalysisService（AI 分析 + SSE 流式） | 1d |
| **Phase 6** | 前端 upload.vue + report.vue | 1.5d |
| **Phase 7** | 面试联动（chat.vue 简历入口 + System Prompt 增强） | 0.5d |
| **Phase 8** | 联调测试 | 1d |
| **合计** | | **7d** |

---

## 10. 风险与注意事项

1. **阿里云凭证安全**: AccessKey 通过环境变量注入，严禁硬编码。若凭据已泄露需立即在 RAM 控制台禁用
2. **解析耗时**: 大文件 (10MB+) 可能解析超过 30s，轮询间隔建议 2s，最多轮询 30 次
3. **AI 输出格式**: 必须用强约束 System Prompt 确保 JSON 可解析，fallback 时建议重试一次
4. **文件存储**: 当前设计解析后不持久存储原始文件（仅存文本），如需保留原文件需要额外 OSS 集成
5. **并发限制**: 阿里云文档智能 QPS 默认为 1，高并发时需排队
