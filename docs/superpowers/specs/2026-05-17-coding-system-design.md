# 笔试编程系统 — 设计规格

> 日期: 2026-05-17 | 状态: 阶段一完成，阶段二三待实施

## 一、概述

在 AI 面试 PC 端三栏布局中集成类 LeetCode 编程实战功能，包括：AI 主动邀请笔试、代码编辑器、Piston 在线执行、AI 代码审查。笔试为可选环节，不影响面试评分。

## 二、架构

```
┌──────────────────────────────────────────────────┐
│                 InterviewView.vue                  │
│  ┌─ Sidebar ─┬─ Chat ─┬─ CodePanel ────────────┐  │
│  │ 面试进度   │ 对话流  │ CodeEditor.vue        │  │
│  │            │        │  [Run Code] [Submit]   │  │
│  │            │        │  执行结果面板           │  │
│  └────────────┴────────┴────────────────────────┘  │
└──────────────────────────────────────────────────┘
         │                  │              │
         ▼                  ▼              ▼
  useInterviewStream   InterviewService   Piston API
  (SSE解析+标记检测)   (出题拦截+审查)    (代码执行)
```

## 三、面试流程

```
自我介绍 → 技术问答(3-5轮)
              ↓
         AI评估良好? ──否──→ 继续问答或结束
              ↓是
         AI输出[笔试邀请]
              ↓
    ┌─────────────────────────────────┐
    │  进入编程实战环节？              │
    │      [跳过]      [进入编程]     │
    └─────────────────────────────────┘
         ↓                    ↓
    发送"跳过编程"        startCoding()
    AI结束+评分正常        ↓
                     后端拦截→独立prompt出题
                           ↓
                     [编程题目]JSON
                           ↓
                     前端解析→编辑器展示
                           ↓
                     用户写代码
                           ↓
              ┌───── [Run Code] ─────┐
              ↓                       ↓
         Piston执行                [提交审查]
         显示通过/失败              SSE发给AI
              ↓                       ↓
         用户修改代码              AI反馈代码质量
              ↓                       ↓
         再次Run Code              AI输出[面试结束]
                                     ↓
                                  报告页展示
```

## 四、技术选型

| 组件 | 技术 | 说明 |
|------|------|------|
| 编辑器 | CodeMirror 6 | @codemirror/view + lang-java/python/js |
| 主题 | oneDark | 适配 Warm Tech 暗色背景 |
| 代码执行 | Piston | Go 单容器 ~300MB，REST API |
| 通信 | SSE | 复用现有面试流式通道 |

### Piston 部署

```bash
docker run -d -p 2000:2000 --restart=always \
  --name piston ghcr.io/engineer-man/piston
```

```yaml
# API: POST /api/v2/execute
{
  "language": "java",
  "version": "15.0.2",
  "files": [
    { "name": "Solution.java", "content": "public class Solution { ... }" }
  ],
  "stdin": "",
  "args": [],
  "compile_timeout": 10000,
  "run_timeout": 3000,
  "compile_memory_limit": -1,
  "run_memory_limit": -1
}

# Response
{
  "run": {
    "stdout": "Hello World\n",
    "stderr": "",
    "code": 0,
    "signal": null,
    "output": "Hello World\n"
  },
  "language": "java",
  "version": "15.0.2"
}
```

## 五、数据结构

### 5.1 SSE 标记定义

| 标记 | 方向 | 触发条件 | 数据结构 |
|------|------|----------|----------|
| `[笔试邀请]` | AI→前端 | AI 评估候选人良好 | 纯文本标记，无 JSON |
| `[进入编程环节]` | 前端→后端 | 用户点击 [进入编程] | 作为用户回答发送 |
| `[编程题目]` | AI→前端 | 出题 AI 生成 | `{"type":"algorithm\|complete","title":"","description":"","template":"","language":"java"}` |
| `[面试结束]` | AI→前端 | 笔试审查完成或跳过 | `{"score":6,"feedback":"",...}` |

### 5.2 题库表 `algorithm_problems`（待创建）

```sql
CREATE TABLE algorithm_problems (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  title         VARCHAR(100)  NOT NULL COMMENT '题目标题',
  description   TEXT          NOT NULL COMMENT '题目描述(展示给用户)',
  difficulty    VARCHAR(10)   NOT NULL DEFAULT 'easy' COMMENT 'easy/medium/hard',
  starter_code  TEXT          NOT NULL COMMENT '代码模板(含函数签名)',
  test_cases    JSON          NOT NULL COMMENT '[{"input":"...","expected":"..."}]',
  solution_code TEXT          NOT NULL COMMENT '参考答案(用户不可见)',
  language      VARCHAR(20)   NOT NULL DEFAULT 'java',
  category      VARCHAR(50)   COMMENT '分类: array/tree/dp/string/...',
  created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP
);
```

## 六、API 设计

### 6.1 代码执行 `POST /api/coding/run`

```
Request:
{
  "code": "public class Solution { ... }",
  "language": "java",
  "testCases": [{"input": "1 2 3", "expected": "6"}]
}

Response:
{
  "results": [
    {"passed": true, "input": "1 2 3", "expected": "6", "actual": "6", "time": "45ms"},
    {"passed": false, "input": "1 2 300", "expected": "6", "actual": "303", "time": "42ms"}
  ],
  "summary": {"total": 2, "passed": 1, "failed": 1}
}
```

### 6.2 代码审查 `POST /api/interview/{id}/answer/stream`（复用）

用户的代码通过 `code` 字段附加到回答中发给 SSE：

```json
{
  "answer": "我写完了，请审查",
  "code": "public class Solution { ... }",
  "codeLang": "java",
  "codeFile": "Solution.java"
}
```

AI 审查后通过 SSE 返回反馈。

## 七、前端组件

### 7.1 CodeEditor.vue（已完成）

| Prop | 类型 | 说明 |
|------|------|------|
| `modelValue` | `string` | v-model 双向绑定代码内容 |
| `filename` | `string` | 标题栏文件名 |
| `language` | `string` | java/python/javascript |
| `readonly` | `boolean` | 只读模式 |

### 7.2 RunCode 面板（待实现）

```
┌─ 执行结果 ───────────────────────────┐
│ 测试1 ✓ 通过  input: [1,2,3]  45ms  │
│ 测试2 ✗ 失败  input: [1,2,300] 42ms │
│   expected: 6  actual: 303           │
│ ──────────────────────────────────── │
│ 通过: 1/2                            │
└──────────────────────────────────────┘
```

### 7.3 邀请栏（已完成）

```
┌──────────────────────────────────────┐
│  进入编程实战环节？                   │
│              [跳过]    [进入编程]     │
└──────────────────────────────────────┘
```

## 八、安全性

| 关注点 | 措施 |
|--------|------|
| 用户代码执行 | Piston nsjail 沙箱，不可访问宿主机 |
| 资源限制 | run_timeout=3s, run_memory_limit=256MB |
| 死循环 | 超时自动 kill |
| 恶意系统调用 | nsjail 白名单机制，禁止网络/文件系统访问 |
| 参考答案泄露 | solution_code 仅后端使用，不返回前端 |

## 九、逐行计划

### 阶段一（已完成）

- [x] CodeMirror 6 编辑器组件
- [x] AI 主动邀请笔试 Prompt
- [x] 前端邀请栏 [进入]/[跳过] 按钮
- [x] 后端拦截出题（handleCodingRound）
- [x] [编程题目] JSON 解析 + 自动填充编辑器

### 阶段二（Run Code）

- [ ] 服务器部署 Piston 容器
- [ ] 后端 CodingController: `POST /api/coding/run`
- [ ] 前端 CodeEditor 添加 [Run Code] 按钮
- [ ] 前端执行结果面板组件

### 阶段三（审查 + 题库）

- [ ] `POST /api/coding/submit` — 提交代码给 AI 审查
- [ ] algorithm_problems 表 + 种子数据（10-20 题）
- [ ] AI 出题时从题库筛选匹配题目
- [ ] [Run Code] 从题库 test_cases 取测试用例执行
