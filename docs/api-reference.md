# 面面通 — 后端 API 参考文档

> Base URL: `http://localhost:8080` | 认证方式: Bearer JWT | 通用响应格式见 §1

---

## 1. 通用约定

### 1.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 / Token 过期 |
| 500 | 服务器内部错误 |

### 1.2 认证头

```
Authorization: Bearer <jwt_token>
```

除白名单路径外，所有接口必须携带。

---

## 2. 认证模块 — `/api/auth`

### 2.1 微信登录

```
POST /api/auth/login
```

**Request Body:**

```json
{
  "code": "微信临时登录code",
  "nickname": "用户昵称（可选）",
  "avatarUrl": "头像URL（可选）"
}
```

**Response (200):**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOi...",
    "userId": 1,
    "nickname": "用户1001",
    "avatarUrl": null
  }
}
```

---

## 3. 题库模块 — `/api/questions`

### 3.1 题库列表（分页 + 筛选）

```
GET /api/questions?categoryId=1&difficulty=2&type=1&page=1&size=10
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | int | 否 | 分类 ID |
| difficulty | int | 否 | 1=简单 2=中等 3=困难 |
| type | int | 否 | 1=单选 2=多选 3=判断 4=填空 |
| page | int | 否 | 页码(默认1) |
| size | int | 否 | 每页条数(默认10) |

**Response:**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "categoryId": 1,
        "categoryName": "计算机网络",
        "type": 1,
        "title": "OSI七层模型中，传输层的作用是什么？",
        "difficulty": 1,
        "tags": "OSI,TCP,基础"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

### 3.2 随机抽题

```
GET /api/questions/random?categoryId=1&difficulty=2&size=10
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | int | 否 | 分类 ID |
| difficulty | int | 否 | 难度筛选 |
| size | int | 否 | 抽题数量(默认10) |

### 3.3 题目详情

```
GET /api/questions/{id}
```

### 3.4 分类列表

```
GET /api/questions/categories
```

**Response:**

```json
{
  "code": 200,
  "data": [
    { "id": 1, "name": "计算机网络", "icon": "network", "sortOrder": 1 },
    { "id": 2, "name": "操作系统", "icon": "os", "sortOrder": 2 }
  ]
}
```

---

## 4. 答题模块 — `/api/answers`

### 4.1 提交答案（单题）

```
POST /api/answers
```

**Request Body:**

```json
{
  "questionId": 1,
  "userAnswer": "B"
}
```

**Response:**

```json
{
  "code": 200,
  "data": {
    "isCorrect": true,
    "correctAnswer": "B",
    "analysis": "传输层负责端到端的可靠数据传输..."
  }
}
```

---

## 5. 考试模块 — `/api/exams`

### 5.1 试卷列表

```
GET /api/exams
```

### 5.2 开始考试

```
POST /api/exams/{examId}/start
```

**Response:**

```json
{
  "code": 200,
  "data": {
    "exam": { "id": 1, "title": "...", "duration": 30, "totalScore": 100 },
    "questions": [
      { "id": 1, "type": 1, "title": "...", "options": "[...]" }
    ]
  }
}
```

> 注意: 题目列表不含 `answer` 和 `analysis` 字段。

### 5.3 提交考试

```
POST /api/exams/{examId}/submit
```

**Request Body:**

```json
{
  "answers": [
    { "questionId": 1, "userAnswer": "B" },
    { "questionId": 2, "userAnswer": "正确" }
  ]
}
```

**Response:**

```json
{
  "code": 200,
  "data": {
    "totalScore": 80,
    "correctCount": 8,
    "totalCount": 10,
    "examTitle": "计算机网络基础测试"
  }
}
```

---

## 6. AI 面试模块 — `/api/interview`

### 6.1 开始面试

```
POST /api/interview/start
```

**Request Body:**

```json
{
  "position": "Java后端开发"
}
```

**Response:**

```json
{
  "code": 200,
  "data": {
    "sessionId": 1,
    "question": "你好，欢迎参加Java后端开发面试...",
    "questionIndex": 1
  }
}
```

可选岗位: `Java后端开发`、`前端开发`、`C++开发`、`Python开发`、`算法工程师`、`测试开发`

### 6.2 回答问题

```
POST /api/interview/{sessionId}/answer
```

**Request Body:**

```json
{
  "answer": "TCP是面向连接的传输协议..."
}
```

**Response (继续):**

```json
{
  "code": 200,
  "data": {
    "sessionId": 1,
    "finished": false,
    "question": "回答得不错，我们深入一下...",
    "questionIndex": 3
  }
}
```

**Response (AI结束面试):**

```json
{
  "code": 200,
  "data": {
    "sessionId": 1,
    "finished": true,
    "report": {
      "score": 7,
      "feedback": "总体基础扎实，表达清晰",
      "dimensions": [
        { "name": "基础掌握", "score": 8, "comment": "概念准确" }
      ],
      "suggestion": "建议深入学习JVM内存模型"
    }
  }
}
```

### 6.3 流式回答 (SSE)

```
POST /api/interview/{sessionId}/answer/stream
```

**Request Body:** 同 §6.2

**SSE 事件流:**

```
event: token
data: 回答

event: token
data: 得不

event: token
data: 错...

event: finish
data: {"finished":true,"report":{"score":7,...}}
```

### 6.4 手动结束面试

```
POST /api/interview/{sessionId}/end
```

**Response:** 同 §6.2 结束响应，AI 基于完整对话生成报告。

### 6.5 面试历史

```
GET /api/interview/list
```

返回最近 5 条面试记录。

### 6.6 面试详情

```
GET /api/interview/{id}
```

返回完整的对话记录、评分、维度分析。

---

## 7. 错题模块 — `/api/wrong-questions`

### 7.1 错题列表

```
GET /api/wrong-questions
```

### 7.2 移除错题（标记已掌握）

```
DELETE /api/wrong-questions/{questionId}
```

---

## 8. 用户模块 — `/api/user`

### 8.1 用户统计

```
GET /api/user/stats
```

**Response:**

```json
{
  "code": 200,
  "data": {
    "practiceCount": 42,
    "interviewCount": 5,
    "wrongCount": 8
  }
}
```

### 8.2 获取 AI 配置

```
GET /api/user/ai-config
```

### 8.3 保存 AI 配置

```
PUT /api/user/ai-config
```

**Request Body:**

```json
{
  "provider": "deepseek",
  "apiKey": "sk-xxxxxxxx"
}
```

---

## 9. AI 服务切换

在 `application.yml` 中配置:

```yaml
ai:
  provider: deepseek   # deepseek | qwen
```

当 `provider=qwen` 时，`QwenAiService` 替代 `DeepSeekAiService` 被注入到 `InterviewService`。
