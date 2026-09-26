## Agent skills

> 下列文档均为**本地私有**（`.gitignore` 排除了 `docs/*`、`CONTEXT.md`、`.scratch/`）。
> 克隆仓库后不会有这些文件，需要本地自行维护。

### Project map

先读 `docs/agents/project-map.md` 定位模块，再用 Grep/Glob 精确命中文件，不要全量扫 `src/**`。

### Issue tracker

Issues and PRDs live as markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary — all five roles map to their canonical names. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context repo — one `CONTEXT.md` at the root + `docs/adr/`. See `docs/agents/domain.md`.

Existing ADRs:

- `docs/adr/0001-ai-gateway-boundary.md` — 所有 LLM 调用经 `AiGateway`，不在业务代码直连 HTTP。
- `docs/adr/0002-single-node-resource-budget.md` — 生产机 2 核 2G，并发与内存的硬约束。
