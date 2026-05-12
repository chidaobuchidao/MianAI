# 面面通 — 数据库设计文档

> 数据库名称: `ai-interview` | 引擎: InnoDB | 字符集: utf8mb4

---

## 1. ER 关系图

```
┌──────────┐       ┌──────────────────┐       ┌──────────┐
│ sys_user │       │ interview_session │       │  exam    │
│──────────│       │──────────────────│       │──────────│
│ id (PK)  │──┐    │ id (PK)          │       │ id (PK)  │
│ openid   │  │    │ user_id (FK) ────┘       │ title    │
│ nickname │  │    │ position         │       │ duration │
│ avatar   │  │    │ messages (JSON)  │       └────┬─────┘
│ role     │  │    │ overall_score    │            │
└────┬─────┘  │    │ dimensions(JSON) │    ┌───────┴────────┐
     │        │    │ feedback         │    │ exam_question  │
     │        │    │ status           │    │────────────────│
     │        │    └──────────────────┘    │ exam_id (FK)   │
     │        │                            │ question_id(FK)│
     │   ┌────┴─────────────┐              └───────┬────────┘
     │   │ user_ai_config   │                      │
     │   │──────────────────│              ┌───────┴────────┐
     │   │ user_id (PK/FK)  │              │   question     │
     │   │ provider         │              │────────────────│
     │   │ api_key          │              │ id (PK)        │
     │   └──────────────────┘              │ category_id(FK)│
     │                                     │ type           │
     │    ┌─────────────────┐              │ title          │
     │    │ answer_record   │              │ options (JSON) │
     │    │─────────────────│              │ answer         │
     │    │ id (PK)         │              │ analysis       │
     ├────│ user_id (FK)    │              │ difficulty     │
     │    │ exam_id (FK)    │              └───────┬────────┘
     │    │ question_id(FK) │                      │
     │    │ user_answer     │              ┌───────┴──────────┐
     │    │ is_correct      │              │ question_category│
     │    │ score           │              │──────────────────│
     │    └─────────────────┘              │ id (PK)          │
     │                                     │ name             │
     │    ┌─────────────────┐              │ icon             │
     │    │  wrong_question │              │ sort_order       │
     │    │─────────────────│              └──────────────────┘
     ├────│ user_id (FK)    │
     │    │ question_id(FK) │              ┌──────────────────┐
     │    │ wrong_count     │              │  user_favorite   │
     │    │ last_wrong_time │              │──────────────────│
     │    └─────────────────┘              │ user_id (FK)     │
     │                                     │ question_id (FK) │
     └─────────────────────────────────────┴──────────────────┘
```

---

## 2. 表结构详述

### 2.1 sys_user — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 用户 ID |
| openid | VARCHAR(64) | NOT NULL, UNIQUE | 微信 OpenID |
| nickname | VARCHAR(50) | — | 昵称 |
| avatar_url | VARCHAR(255) | — | 头像 URL |
| role | TINYINT | DEFAULT 0 | 0=学生, 1=管理员 |
| create_time | DATETIME | DEFAULT NOW() | 创建时间 |
| update_time | DATETIME | ON UPDATE NOW() | 更新时间 |

### 2.2 question_category — 题目分类表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 分类 ID |
| name | VARCHAR(50) | NOT NULL | 分类名称 |
| icon | VARCHAR(50) | — | 图标标识 |
| sort_order | INT | DEFAULT 0 | 排序 |
| create_time | DATETIME | DEFAULT NOW() | 创建时间 |

预置分类：计算机网络、操作系统、数据结构、算法、数据库、Java、设计模式、计算机组成原理

### 2.3 question — 题库表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 题目 ID |
| category_id | BIGINT | NOT NULL, INDEX | 分类 ID |
| type | TINYINT | NOT NULL | 1=单选 2=多选 3=判断 4=填空 |
| title | TEXT | NOT NULL | 题目标题 |
| options | JSON | — | 选择题选项 `[{label,content}]` |
| answer | VARCHAR(500) | NOT NULL | 正确答案 |
| analysis | TEXT | — | 题目解析 |
| difficulty | TINYINT | DEFAULT 1, INDEX | 1=简单 2=中等 3=困难 |
| tags | VARCHAR(200) | — | 标签（逗号分隔） |
| create_time | DATETIME | DEFAULT NOW() | 创建时间 |

索引: `idx_category(category_id)`, `idx_difficulty(difficulty)`

### 2.4 exam — 试卷表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 试卷 ID |
| title | VARCHAR(100) | NOT NULL | 试卷标题 |
| description | VARCHAR(500) | — | 试卷描述 |
| duration | INT | — | 考试时长（分钟） |
| total_score | INT | DEFAULT 100 | 总分 |
| status | TINYINT | DEFAULT 1 | 0=禁用 1=启用 |
| create_time | DATETIME | DEFAULT NOW() | 创建时间 |

### 2.5 exam_question — 试卷题目关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| exam_id | BIGINT | PK (联合) | 试卷 ID |
| question_id | BIGINT | PK (联合) | 题目 ID |
| sort_order | INT | DEFAULT 0 | 题目排序 |

### 2.6 answer_record — 答题记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 记录 ID |
| user_id | BIGINT | NOT NULL, INDEX | 用户 ID |
| exam_id | BIGINT | — | 试卷 ID（刷题为 NULL） |
| question_id | BIGINT | NOT NULL | 题目 ID |
| user_answer | VARCHAR(500) | — | 用户答案 |
| is_correct | TINYINT | — | 0=错误 1=正确 |
| score | INT | DEFAULT 0 | 得分 |
| create_time | DATETIME | DEFAULT NOW() | 答题时间 |

索引: `idx_user_id(user_id)`, `idx_user_question(user_id, question_id)`

### 2.7 wrong_question — 错题本

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | BIGINT | PK (联合) | 用户 ID |
| question_id | BIGINT | PK (联合) | 题目 ID |
| wrong_count | INT | DEFAULT 1 | 错误次数 |
| last_wrong_time | DATETIME | — | 最近错题时间 |

### 2.8 user_favorite — 用户收藏表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | BIGINT | PK (联合) | 用户 ID |
| question_id | BIGINT | PK (联合) | 题目 ID |
| create_time | DATETIME | DEFAULT NOW() | 收藏时间 |

### 2.9 interview_session — AI 面试会话表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO_INCREMENT | 会话 ID |
| user_id | BIGINT | NOT NULL, INDEX | 用户 ID |
| position | VARCHAR(50) | — | 面试岗位 |
| messages | JSON | — | 对话记录 `[{role,content,time}]` |
| current_question_index | INT | DEFAULT 0 | 当前问题序号 |
| overall_score | INT | — | AI 综合评分 (1-10) |
| dimensions | JSON | — | 维度评分 `[{name,score,comment}]` |
| feedback | TEXT | — | 总评语 |
| status | TINYINT | DEFAULT 0 | 0=进行中 1=已完成 |
| create_time | DATETIME | DEFAULT NOW() | 开始时间 |
| finish_time | DATETIME | — | 结束时间 |

### 2.10 user_ai_config — 用户 AI 配置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | BIGINT | PK | 用户 ID |
| provider | VARCHAR(20) | DEFAULT 'deepseek' | deepseek / qwen |
| api_key | VARCHAR(255) | NOT NULL | 用户自定义 API Key |
| create_time | DATETIME | DEFAULT NOW() | 创建时间 |
| update_time | DATETIME | ON UPDATE NOW() | 更新时间 |

---

## 3. 关键设计说明

**JSON 字段使用**: `question.options`、`interview_session.messages`、`interview_session.dimensions` 使用 MySQL JSON 类型存储动态结构数据，避免了额外的关联表，适合读多写少场景。

**错题去重**: `wrong_question` 表使用 `(user_id, question_id)` 联合主键，配合 `wrong_count` 字段实现重复错题计数而非重复插入。

**会话清理策略**: `InterviewService.cleanupOldSessions()` 限制每个用户最多保留 5 条历史会话，按时间降序保留最新记录。

**判分逻辑**: `ExamService.judge()` 中：单选题/多选题/判断题使用精确匹配（忽略大小写），填空题使用包含匹配（忽略大小写）。每题固定 10 分。
