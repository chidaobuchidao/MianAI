package com.mianmiantong.service.document;

import com.mianmiantong.dto.paper.PaperExportRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于原始 .docx 模板的格式保留导出服务。
 * 采用段落 ID 标记法：逐段匹配改写结果，保留原文档格式。
 */
@Slf4j
@Service
public class TemplatePreservingExportService {

    // ======================== 段落定位与导出 ========================

    private final DocxTextLocator locator = new DocxTextLocator();
    private final DocxPatchApplier patchApplier = new DocxPatchApplier();
    private static final Pattern FIELD_LABEL_PATTERN = Pattern.compile(
        "(^|[\\s;；,，|、])([\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z0-9/（）()\\- ]{0,12})[：:]");
    private static final String[] PERSONAL_INFO_LABELS = {
        "出生年月", "籍贯", "民族", "政治面貌", "学历", "手机", "现居地", "工作年限", "邮箱"
    };
    private static final String[] RESUME_SECTION_LABELS = {
        "求职意向", "教育背景", "教育经历", "在校经历", "工作经历", "项目经历", "实习经历",
        "个人技能", "专业技能", "自我评价", "个人信息", "基本信息", "联系方式"
    };
    private static final String OBJECTIVE_LABEL = "求职意向";

    private record OptimizedCandidate(int index, String text, String normalized, int significantLength) {}

    /** 从原始 DOCX 解析所有可改写段落的格式快照。委托给 {@link DocxTextLocator}。 */
    public List<ParagraphProfile> parseParagraphs(byte[] originalDocx) {
        return locator.locate(originalDocx);
    }

    /** 回退全文本提取：当段落解析为空时，提取所有文本（包括表格和短文本） */
    public String extractFallbackText(byte[] docxFile) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxFile))) {
            StringBuilder sb = new StringBuilder();
            // 正文
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = extractAllText(para);
                if (!text.isBlank()) sb.append(text).append("\n\n");
            }
            // 表格 → 每行用 tab 连接，行间换行
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cellTexts = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getParagraphs().stream()
                            .map(this::extractAllText)
                            .filter(t -> !t.isBlank())
                            .collect(Collectors.joining(" "));
                        if (!cellText.isBlank()) cellTexts.add(cellText);
                    }
                    if (!cellTexts.isEmpty()) {
                        sb.append(String.join("\t", cellTexts)).append("\n");
                    }
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Fallback text extraction failed", e);
            return "";
        }
    }

    /** 从段落中提取所有文本，包括被格式分割的片段 */
    private String extractAllText(XWPFParagraph para) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t != null) sb.append(t);
        }
        String text = sb.toString();
        if (text.isBlank()) text = para.getText();
        return text != null ? text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim() : "";
    }

    /**
     * 将改写后的段落写回原 DOCX，保留所有格式。
     * 使用 path-based 定位 + before 校验 + 首 run 整合替换文本。
     *
     * @param skipTableMatching true=跳过表格段落（简历场景，避免误改表格模板）
     */
    public byte[] writeBack(byte[] originalDocx, Map<Integer, String> rewrittenParagraphs,
                            boolean skipTableMatching) {
        // Step 1: 定位所有段落，获取 path + 原文 + 文本框文本映射
        DocxTextLocator.LocatorResult locatorResult = locator.locateWithTextBoxMap(originalDocx);
        List<ParagraphProfile> profiles = locatorResult.profiles();
        Map<String, DocxPath> textBoxTextToPath = locatorResult.textBoxTextToPath();
        Map<Integer, ParagraphProfile> profileByIndex = new LinkedHashMap<>();
        for (ParagraphProfile p : profiles) {
            profileByIndex.put(p.index(), p);
        }

        // Step 2: 构建 patch 列表
        List<DocxPatch> patches = new ArrayList<>();
        for (Map.Entry<Integer, String> e : rewrittenParagraphs.entrySet()) {
            int idx = e.getKey();
            ParagraphProfile profile = profileByIndex.get(idx);
            if (profile == null) {
                log.info("writeBack: 段落[{}] 未在profileByIndex中找到", idx);
                continue;
            }
            DocxPath path = profile.path();
            if (path == null) {
                log.info("writeBack: 段落[{}] path为null", idx);
                continue;
            }

            // 简历场景跳过表格段落
            if (skipTableMatching && path.pathString().contains(".table[")) continue;

            patches.add(new DocxPatch(path, profile.text(), e.getValue()));
            log.info("writeBack patch: path={}, before='{}', after='{}'",
                path.pathString(),
                profile.text().length() > 20 ? profile.text().substring(0, 20) + "..." : profile.text(),
                e.getValue().length() > 20 ? e.getValue().substring(0, 20) + "..." : e.getValue());
        }

        if (patches.isEmpty()) {
            throw new RuntimeException("没有可匹配的段落，无法保留格式导出。请确认文档未被修改。");
        }

        // Step 3: 应用 patches（内部做 before 校验 + 文本框DOM直改）
        return patchApplier.applyPatches(originalDocx, patches, textBoxTextToPath);
    }

    /**
     * 片段级写回：只替换段落中匹配的 before 片段，保留段落其余内容和格式。
     * 用于简历导出场景，highlights.before 是段落中的一个片段，after 是优化后的片段。
     *
     * @param snippetMappings key=段落索引, value=[before片段, after文本]
     */
    public byte[] writeBackSnippets(byte[] originalDocx, Map<Integer, String[]> snippetMappings,
                                    boolean skipTableMatching) {
        DocxTextLocator.LocatorResult locatorResult = locator.locateWithTextBoxMap(originalDocx);
        List<ParagraphProfile> profiles = locatorResult.profiles();
        Map<String, DocxPath> textBoxTextToPath = locatorResult.textBoxTextToPath();
        Map<Integer, ParagraphProfile> profileByIndex = new LinkedHashMap<>();
        for (ParagraphProfile p : profiles) {
            profileByIndex.put(p.index(), p);
        }

        List<DocxPatch> patches = new ArrayList<>();
        for (Map.Entry<Integer, String[]> e : snippetMappings.entrySet()) {
            int idx = e.getKey();
            String[] pair = e.getValue();
            if (pair == null || pair.length < 2) continue;
            String beforeSnippet = pair[0];
            String afterText = pair[1];
            if (beforeSnippet == null || beforeSnippet.isBlank() || afterText == null || afterText.isBlank()) continue;

            ParagraphProfile profile = profileByIndex.get(idx);
            if (profile == null) continue;
            DocxPath path = profile.path();
            if (path == null) continue;
            if (skipTableMatching && path.pathString().contains(".table[")) continue;

            patches.add(new DocxPatch(path, beforeSnippet, afterText));
            log.info("writeBackSnippets patch: path={}, before='{}', after='{}'",
                path.pathString(),
                beforeSnippet.length() > 30 ? beforeSnippet.substring(0, 30) + "..." : beforeSnippet,
                afterText.length() > 30 ? afterText.substring(0, 30) + "..." : afterText);
        }

        if (patches.isEmpty()) {
            throw new RuntimeException("没有可匹配的片段，无法保留格式导出。");
        }

        return patchApplier.applyPatches(originalDocx, patches, textBoxTextToPath);
    }

    /** 完整导出流程：接收前端传来的段落映射，直接写回 */
    public byte[] exportWithPreservedFormat(byte[] originalDocx, PaperExportRequest request) {
        List<ParagraphProfile> profiles = parseParagraphs(originalDocx);
        Map<Integer, ParagraphProfile> profileByIndex = new LinkedHashMap<>();
        for (ParagraphProfile profile : profiles) {
            profileByIndex.put(profile.index(), profile);
        }

        Map<Integer, String> changedParaMap = new LinkedHashMap<>();
        if (request.getParagraphs() != null) {
            for (PaperExportRequest.ParagraphMapping pm : request.getParagraphs()) {
                ParagraphProfile original = profileByIndex.get(pm.getIndex());
                if (original == null) {
                    log.info("paper export: skip paragraph[{}], original profile not found", pm.getIndex());
                    continue;
                }

                String nextText = pm.getText();
                if (nextText == null || nextText.isBlank()) {
                    log.info("paper export: skip paragraph[{}], replacement is empty", pm.getIndex());
                    continue;
                }

                if (isSameTextForExport(original.text(), nextText)) {
                    continue;
                }

                changedParaMap.put(pm.getIndex(), nextText);
            }
        }

        log.info("paper preserve-format export: changed paragraphs {}/{}",
            changedParaMap.size(), request.getParagraphs() != null ? request.getParagraphs().size() : 0);
        if (changedParaMap.isEmpty()) {
            return originalDocx;
        }

        return writeBack(originalDocx, changedParaMap, false);
    }

    public byte[] exportConvertedPdfDocx(byte[] convertedDocx, PaperExportRequest request) {
        if (request.getParagraphs() == null || request.getParagraphs().isEmpty()) {
            return convertedDocx;
        }

        List<ParagraphProfile> profiles = parseParagraphs(convertedDocx);
        long rewritableCount = profiles.stream().filter(ParagraphProfile::isRewritable).count();
        if (rewritableCount == 0) {
            log.warn("pdf-to-word beta export: no rewritable paragraphs in converted DOCX");
            return convertedDocx;
        }

        // 使用原文精确匹配：用 originalText 匹配转换后的 DOCX 段落，匹配成功后用 text 替换
        Map<Integer, String> mappings = buildMappingsByOriginalTextMatch(profiles, request.getParagraphs());

        if (mappings.isEmpty()) {
            log.warn("pdf-to-word beta export: no paragraphs matched by original text, returning converted DOCX");
            return convertedDocx;
        }

        log.info("pdf-to-word beta export: patching {}/{} paragraphs by original text match",
            mappings.size(), rewritableCount);
        return writeBack(convertedDocx, mappings, false);
    }

    private String buildOptimizedText(PaperExportRequest request) {
        if (request.getParagraphs() == null || request.getParagraphs().isEmpty()) {
            return "";
        }

        return request.getParagraphs().stream()
            .map(PaperExportRequest.ParagraphMapping::getText)
            .filter(text -> text != null && !text.isBlank())
            .collect(Collectors.joining("\n\n"));
    }

    /**
     * 原文精确匹配：用前端传来的 originalText 匹配转换后 DOCX 的段落，
     * 匹配成功后用优化后的 text 替换。
     * 因为 originalText 来自原始 PDF，与转换后 DOCX 文字几乎一致，
     * 所以用高阈值（0.85）精确匹配即可，不需要模糊匹配。
     */
    private Map<Integer, String> buildMappingsByOriginalTextMatch(
            List<ParagraphProfile> profiles,
            List<PaperExportRequest.ParagraphMapping> paragraphs) {

        Map<Integer, String> mappings = new LinkedHashMap<>();
        Set<Integer> usedProfileIndices = new HashSet<>();

        for (PaperExportRequest.ParagraphMapping pm : paragraphs) {
            String originalText = pm.getOriginalText();
            String optimizedText = pm.getText();

            // 跳过没有原文或优化文本为空的段落
            if (originalText == null || originalText.isBlank()) continue;
            if (optimizedText == null || optimizedText.isBlank()) continue;

            // 跳过原文和优化文本相同的段落（未修改）
            if (isSameTextForExport(originalText, optimizedText)) continue;

            String normOrig = DocxTextUtils.normalize(originalText);
            if (normOrig.isEmpty()) continue;

            // 在转换后的 DOCX 中查找匹配的段落
            int bestIdx = -1;
            double bestScore = 0;

            for (ParagraphProfile profile : profiles) {
                if (usedProfileIndices.contains(profile.index())) continue;
                if (!profile.isRewritable()) continue;

                String normConverted = DocxTextUtils.normalize(profile.text());
                if (normConverted.isEmpty()) continue;

                // 双向子串检测
                if (normOrig.contains(normConverted) || normConverted.contains(normOrig)) {
                    bestIdx = profile.index();
                    bestScore = 1.0;
                    break;
                }

                // 高阈值 LCS 相似度
                double sim = DocxTextUtils.similarity(normOrig, normConverted);
                if (sim > bestScore) {
                    bestScore = sim;
                    bestIdx = profile.index();
                }
            }

            // 高阈值：原文和转换后文字应高度一致
            if (bestIdx >= 0 && bestScore >= 0.85) {
                mappings.put(bestIdx, optimizedText);
                usedProfileIndices.add(bestIdx);
                log.info("  原文匹配: 段落[{}] '{}' => 优化 '{}' (score={})",
                    bestIdx,
                    originalText.length() > 30 ? originalText.substring(0, 30) + "..." : originalText,
                    optimizedText.length() > 30 ? optimizedText.substring(0, 30) + "..." : optimizedText,
                    String.format("%.2f", bestScore));
            } else {
                log.info("  原文未匹配: '{}' (bestScore={}, bestIdx={})",
                    originalText.length() > 40 ? originalText.substring(0, 40) + "..." : originalText,
                    String.format("%.2f", bestScore), bestIdx);
            }
        }

        return mappings;
    }

    private boolean isSameTextForExport(String originalText, String nextText) {
        String original = originalText == null ? "" : originalText.trim();
        String next = nextText == null ? "" : nextText.trim();
        return original.equals(next) || DocxTextUtils.normalize(original).equals(DocxTextUtils.normalize(next));
    }

    /** 标准导出：纯文本生成 DOCX（回退方案，不保留原格式） */
    public byte[] generateStandardDocx(String text) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String[] paragraphs = text.split("\n\n");
            for (String paraText : paragraphs) {
                String trimmed = paraText.trim();
                if (trimmed.isEmpty()) continue;
                XWPFParagraph para = doc.createParagraph();
                para.setFirstLineIndent(480);
                XWPFRun run = para.createRun();
                run.setText(trimmed);
                run.setFontFamily("Times New Roman");
                run.setFontSize(12);
            }

            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Standard DOCX generation failed", e);
            throw new RuntimeException("标准导出失败", e);
        }
    }

    // ======================== 语义匹配 ========================

    private String normalizeForMatch(String text) {
        return DocxTextUtils.normalize(text);
    }

    private double textSimilarity(String a, String b) {
        return DocxTextUtils.similarity(a, b);
    }

    /**
     * 在段落列表中查找与 searchText 最匹配的段落索引。
     * @param searchText 搜索文本
     * @param profiles 段落格式快照列表
     * @param threshold 相似度阈值 (0.0~1.0)
     * @param excludeIndices 排除的索引（已匹配的段落不再参与匹配）
     * @return 最佳匹配的段落索引，无匹配返回 -1
     */
    public int findBestMatch(String searchText, List<ParagraphProfile> profiles,
                              double threshold, Set<Integer> excludeIndices) {
        String normalizedSearch = normalizeForMatch(searchText);
        if (normalizedSearch.isEmpty()) return -1;

        int bestIdx = -1;
        double bestScore = 0;

        for (ParagraphProfile p : profiles) {
            if (excludeIndices != null && excludeIndices.contains(p.index())) continue;
            String normalizedProfile = normalizeForMatch(p.text());
            if (normalizedProfile.isEmpty()) continue;

            double score = textSimilarity(normalizedSearch, normalizedProfile);
            if (score > bestScore && score >= threshold) {
                bestScore = score;
                bestIdx = p.index();
            }
        }
        return bestIdx;
    }

    /**
     * 使用 AI 返回的 highlights 构建段落映射。
     * before 是原文精确引用（15-50字），先用精确子串匹配定位段落，
     * 未命中再回退 LCS 模糊匹配。
     */
    public Map<Integer, String> buildMappingsFromHighlights(
            List<ParagraphProfile> profiles,
            List<Map<String, Object>> highlights) {
        Map<Integer, String> mappings = new LinkedHashMap<>();
        if (highlights == null || highlights.isEmpty()) return mappings;

        Set<Integer> usedIndices = new HashSet<>();

        for (int hi = 0; hi < highlights.size(); hi++) {
            Map<String, Object> h = highlights.get(hi);
            String before = (String) h.get("before");
            String after = (String) h.get("after");
            if (before == null || before.isBlank() || after == null || after.isBlank()) {
                log.info("highlight[{}] 跳过: before或after为空", hi);
                continue;
            }

            String normBefore = normalizeForMatch(before);

            // Pass 1: exact substring match (normalized). Prefer the shortest
            // containing paragraph so outer text-box containers do not win over
            // the real leaf paragraph.
            int matchedIdx = -1;
            int shortestContainingLength = Integer.MAX_VALUE;
            for (ParagraphProfile p : profiles) {
                if (usedIndices.contains(p.index())) continue;
                String normText = normalizeForMatch(p.text());
                if (normText.contains(normBefore) && normText.length() < shortestContainingLength) {
                    matchedIdx = p.index();
                    shortestContainingLength = normText.length();
                }
            }
            if (matchedIdx >= 0) {
                ParagraphProfile matched = null;
                for (ParagraphProfile p : profiles) {
                    if (p.index() == matchedIdx) {
                        matched = p;
                        break;
                    }
                }
                log.info("  highlight[{}] 精确匹配到段落[{}]: '{}'", hi, matchedIdx,
                    matched != null && matched.text().length() > 30
                        ? matched.text().substring(0, 30) + "..."
                        : matched != null ? matched.text() : "");
            }

            // Pass 2: LCS fuzzy match
            if (matchedIdx < 0) {
                double threshold = normBefore.length() < 10 ? 0.5 : 0.35;
                matchedIdx = findBestMatch(before, profiles, threshold, usedIndices);
                if (matchedIdx >= 0) {
                    log.info("  highlight[{}] 模糊匹配到段落[{}]", hi, matchedIdx);
                }
            }

            if (matchedIdx >= 0) {
                mappings.put(matchedIdx, after);
                usedIndices.add(matchedIdx);
            } else {
                log.info("highlight[{}] 未匹配: before='{}', profiles总数={}", hi,
                    before.length() > 40 ? before.substring(0, 40) + "..." : before, profiles.size());
            }
        }

        log.info("highlights匹配: {}/{} 条成功", mappings.size(), highlights.size());
        return mappings;
    }

    /**
     * 片段级 highlights 匹配：返回每个段落的 [before片段, after片段]，用于 writeBackSnippets。
     * 与 buildMappingsFromHighlights 不同，这里保留 before 片段，只替换段落中的对应部分。
     */
    public Map<Integer, String[]> buildSnippetMappingsFromHighlights(
        List<ParagraphProfile> profiles,
        List<Map<String, Object>> highlights) {
        Map<Integer, String[]> mappings = new LinkedHashMap<>();
        if (highlights == null || highlights.isEmpty()) return mappings;

        Set<Integer> usedIndices = new HashSet<>();

        for (int hi = 0; hi < highlights.size(); hi++) {
            Map<String, Object> h = highlights.get(hi);
            String before = (String) h.get("before");
            String after = (String) h.get("after");
            if (before == null || before.isBlank() || after == null || after.isBlank()) continue;

            String normBefore = normalizeForMatch(before);

            // Pass 1: exact substring match
            int matchedIdx = -1;
            int shortestContainingLength = Integer.MAX_VALUE;
            for (ParagraphProfile p : profiles) {
                if (usedIndices.contains(p.index())) continue;
                String normText = normalizeForMatch(p.text());
                if (normText.contains(normBefore) && normText.length() < shortestContainingLength) {
                    matchedIdx = p.index();
                    shortestContainingLength = normText.length();
                }
            }

            // Pass 2: LCS fuzzy match
            if (matchedIdx < 0) {
                double threshold = normBefore.length() < 10 ? 0.5 : 0.35;
                matchedIdx = findBestMatch(before, profiles, threshold, usedIndices);
            }

            if (matchedIdx >= 0) {
                if (snippetWouldDamageTextboxLayout(before, after, profiles, matchedIdx)) {
                    log.info("highlight[{}] 跳过: replacement may damage textbox layout", hi);
                    continue;
                }
                mappings.put(matchedIdx, new String[]{before, after});
                usedIndices.add(matchedIdx);
            }
        }

        log.info("snippetHighlights匹配: {}/{} 条成功", mappings.size(), highlights.size());
        return mappings;
    }

    /**
     * Build a focused snippet mapping for the objective line.
     *
     * <p>Resume templates often place the name, objective, and personal/contact
     * fields in one textbox. Replacing that whole textbox destroys spacing, so
     * only patch the "求职意向：..." phrase and leave the surrounding layout intact.</p>
     */
    public Map<Integer, String[]> buildObjectiveSnippetMappings(
            List<ParagraphProfile> profiles,
            String optimizedText) {
        Map<Integer, String[]> mappings = new LinkedHashMap<>();
        if (profiles == null || profiles.isEmpty() || optimizedText == null || optimizedText.isBlank()) {
            return mappings;
        }

        String optimizedObjective = extractObjectiveSnippet(optimizedText);
        if (!isSafeObjectiveSnippet(optimizedObjective)) {
            return mappings;
        }

        for (ParagraphProfile profile : profiles) {
            if (profile == null || !profile.isRewritable()) continue;
            String originalObjective = extractObjectiveSnippet(profile.text());
            if (!isSafeObjectiveSnippet(originalObjective)) continue;
            if (isSameTextForExport(originalObjective, optimizedObjective)) continue;

            mappings.put(profile.index(), new String[]{originalObjective, optimizedObjective});
            log.info("objective snippet match: paragraph[{}] '{}' => '{}'",
                profile.index(),
                truncateForLog(originalObjective, 30),
                truncateForLog(optimizedObjective, 30));
            break;
        }

        return mappings;
    }

    /**
     * 内容匹配：将原始 DOCX 段落逐段与 optimizedText 做相似度匹配。
     * 方向为「原文→优化文本」，包含表格段落。
     * 使用高阈值（0.65）+ 语言一致性 + 长度比校验，防止误匹配导致内容错乱。
     *
     * @return index → 优化文本 的映射，可直接传给 {@link #writeBack}
     */
    public Map<Integer, String> buildMappingsByContentMatch(
            List<ParagraphProfile> profiles, String optimizedText) {
        Map<Integer, String> mappings = new LinkedHashMap<>();
        if (optimizedText == null || optimizedText.isBlank()) return mappings;

        // 将 optimizedText 拆段并预处理
        String plain = DocxTextUtils.stripMarkdown(optimizedText);
        String[] optParas = plain.split("\\n\\n+");
        String[] normalizedOpt = new String[optParas.length];
        int[] optLang = new int[optParas.length]; // 0=mixed, 1=mostly CJK, 2=mostly Latin
        for (int i = 0; i < optParas.length; i++) {
            normalizedOpt[i] = DocxTextUtils.normalize(optParas[i]);
            optLang[i] = detectLanguage(optParas[i]);
        }
        boolean[] used = new boolean[optParas.length];

        for (ParagraphProfile profile : profiles) {
            if (!profile.isRewritable()) continue;

            String normOrig = DocxTextUtils.normalize(profile.text());
            if (normOrig.isEmpty()) continue;

            int origLang = detectLanguage(profile.text());

            int bestIdx = -1;
            double bestScore = 0;

            for (int j = 0; j < optParas.length; j++) {
                if (used[j] || normalizedOpt[j].isEmpty()) continue;

                // 语言一致性检查：CJK 段落不应被 Latin 段落替换，反之亦然
                if (origLang != 0 && optLang[j] != 0 && origLang != optLang[j]) continue;

                // 双向子串检测（要求子串至少占较长文本的 30% 长度，防止短串误匹配）
                int minLen = Math.min(normOrig.length(), normalizedOpt[j].length());
                int maxLen = Math.max(normOrig.length(), normalizedOpt[j].length());
                boolean lengthRatioOk = maxLen == 0 || (double) minLen / maxLen >= 0.30;

                if (lengthRatioOk) {
                    boolean origContainsOpt = normOrig.contains(normalizedOpt[j]);
                    boolean optContainsOrig = normalizedOpt[j].contains(normOrig);
                    if (origContainsOpt || optContainsOrig) {
                        bestIdx = j;
                        bestScore = 1.0;
                        break;
                    }
                }

                // LCS 相似度
                double sim = DocxTextUtils.similarity(normOrig, normalizedOpt[j]);
                if (sim > bestScore) {
                    bestScore = sim;
                    bestIdx = j;
                }
            }

            // 高阈值匹配：0.65 防止跨语言/跨段落误匹配
            if (bestIdx >= 0 && bestScore >= 0.65) {
                String matchedText = optParas[bestIdx].trim();
                mappings.put(profile.index(), matchedText);
                used[bestIdx] = true;
                log.info("  匹配: 段落[{}] '{}' <=> 优化[{}] '{}' (score={})",
                    profile.index(),
                    profile.text().length() > 30 ? profile.text().substring(0, 30) + "..." : profile.text(),
                    bestIdx,
                    matchedText.length() > 30 ? matchedText.substring(0, 30) + "..." : matchedText,
                    String.format("%.2f", bestScore));
            }
        }

        log.info("内容匹配: {}/{} 段匹配成功 (optimizedText {} 段)",
            mappings.size(), profiles.size(), optParas.length);
        return mappings;
    }

    /**
     * Safe supplement for resume template export.
     *
     * <p>Highlights are still the primary source of truth. This method only fills
     * unmapped paragraphs from single-line optimized text candidates, and rejects
     * long merged sections so a template textbox is not replaced by an entire
     * resume section.</p>
     */
    public Map<Integer, String> buildSafeResumeSupplementMappings(
            List<ParagraphProfile> profiles,
            String optimizedText,
            Map<Integer, String> existingMappings) {

        Map<Integer, String> mappings = new LinkedHashMap<>();
        if (profiles == null || profiles.isEmpty() || optimizedText == null || optimizedText.isBlank()) {
            return mappings;
        }

        List<OptimizedCandidate> candidates = splitOptimizedCandidates(optimizedText);
        if (candidates.isEmpty()) return mappings;

        Set<Integer> usedCandidateIndices = new HashSet<>();
        if (existingMappings != null && !existingMappings.isEmpty()) {
            markExistingCandidates(candidates, existingMappings, usedCandidateIndices);
        }

        Set<Integer> existingProfileIndices = existingMappings == null ? Set.of() : existingMappings.keySet();

        for (ParagraphProfile profile : profiles) {
            if (profile == null || !profile.isRewritable()) continue;

            String original = stripMarkdown(profile.text());
            String normalizedOriginal = normalizeForMatch(original);
            if (normalizedOriginal.isBlank()) continue;
            boolean alreadyMapped = existingProfileIndices.contains(profile.index());
            String currentMappedText = alreadyMapped ? existingMappings.get(profile.index()) : null;

            OptimizedCandidate best = null;
            double bestScore = 0;
            for (OptimizedCandidate candidate : candidates) {
                if (!alreadyMapped && usedCandidateIndices.contains(candidate.index())) continue;
                if (alreadyMapped && !canImproveExistingResumeMapping(original, currentMappedText, candidate)) continue;
                if (!isSafeResumeReplacement(original, normalizedOriginal, candidate)) continue;

                double score = resumeReplacementScore(original, normalizedOriginal, candidate);
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            if (best != null && bestScore >= safeResumeThreshold(normalizedOriginal.length())) {
                if (!isSameTextForExport(original, best.text())
                    && (!alreadyMapped || shouldReplaceExistingResumeMapping(currentMappedText, best.text()))) {
                    mappings.put(profile.index(), best.text());
                    usedCandidateIndices.add(best.index());
                    log.info("resume supplement match: paragraph[{}] '{}' => '{}' (score={})",
                        profile.index(),
                        truncateForLog(original, 30),
                        truncateForLog(best.text(), 30),
                        String.format("%.2f", bestScore));
                }
            }
        }

        log.info("resume supplement mappings: {} added from {} candidates", mappings.size(), candidates.size());
        return mappings;
    }

    private List<OptimizedCandidate> splitOptimizedCandidates(String optimizedText) {
        String plain = stripMarkdown(optimizedText)
            .replace("\r\n", "\n")
            .replace('\r', '\n');
        String[] lines = plain.split("\\n+");
        List<OptimizedCandidate> candidates = new ArrayList<>();
        for (String line : lines) {
            String cleaned = cleanOptimizedCandidate(line);
            if (cleaned.isBlank()) continue;
            String normalized = normalizeForMatch(cleaned);
            if (normalized.isBlank()) continue;
            if (looksLikeMergedResumeSection(cleaned)) continue;
            candidates.add(new OptimizedCandidate(
                candidates.size(),
                cleaned,
                normalized,
                countSignificantForResume(cleaned)));
        }
        return candidates;
    }

    private String cleanOptimizedCandidate(String line) {
        if (line == null) return "";
        return line
            .replaceAll("^\\s*\\[P\\d+]\\s*", "")
            .replaceAll("^\\s*#{1,6}\\s*", "")
            .replaceAll("^\\s*[-*+]\\s*", "")
            .replaceAll("^\\s*\\d+[.)、]\\s*", "")
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
            .trim();
    }

    private String extractObjectiveSnippet(String text) {
        if (text == null || text.isBlank()) return "";
        String plain = stripMarkdown(text)
            .replace("\r\n", "\n")
            .replace('\r', '\n');
        for (String line : plain.split("\\n+")) {
            String cleaned = cleanOptimizedCandidate(line);
            int objectiveIndex = cleaned.indexOf(OBJECTIVE_LABEL);
            if (objectiveIndex < 0) continue;
            String snippet = cleaned.substring(objectiveIndex).trim();
            snippet = trimAtFollowingResumeLabel(snippet).trim();
            return snippet;
        }
        return "";
    }

    private String trimAtFollowingResumeLabel(String snippet) {
        if (snippet == null || snippet.isBlank()) return "";
        int earliest = snippet.length();
        for (String label : PERSONAL_INFO_LABELS) {
            earliest = earliestFollowingLabelIndex(snippet, label, earliest);
        }
        for (String label : RESUME_SECTION_LABELS) {
            if (OBJECTIVE_LABEL.equals(label)) continue;
            earliest = earliestFollowingLabelIndex(snippet, label, earliest);
        }
        return snippet.substring(0, earliest);
    }

    private int earliestFollowingLabelIndex(String text, String label, int currentEarliest) {
        if (text == null || label == null || label.isBlank()) return currentEarliest;
        int idx = text.indexOf(label);
        return idx > 0 ? Math.min(currentEarliest, idx) : currentEarliest;
    }

    private boolean isSafeObjectiveSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) return false;
        String normalized = normalizeForMatch(snippet);
        if (normalized.isBlank() || !normalized.startsWith(normalizeForMatch(OBJECTIVE_LABEL))) {
            return false;
        }
        if (countKnownLabels(normalized, PERSONAL_INFO_LABELS) > 0) {
            return false;
        }
        if (countKnownLabels(normalized, RESUME_SECTION_LABELS) > 1) {
            return false;
        }
        int labelSeparator = firstIndexOfAny(snippet, '：', ':');
        return labelSeparator > 0
            && labelSeparator < snippet.length() - 1
            && countSignificantForResume(snippet.substring(labelSeparator + 1)) >= 2
            && snippet.length() <= 80;
    }

    private boolean looksLikeMergedResumeSection(String text) {
        if (text == null) return false;
        int sectionSignals = 0;
        String normalized = text.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase();
        String[] markers = {
            "教育背景", "教育经历", "在校经历", "工作经历", "项目经历", "实习经历",
            "个人技能", "专业技能", "自我评价", "求职意向",
            "education", "experience", "skills", "projects", "summary", "objective"
        };
        for (String marker : markers) {
            if (normalized.contains(marker.toLowerCase())) sectionSignals++;
            if (sectionSignals >= 2) return true;
        }
        return text.length() > 120 && sectionSignals >= 1;
    }

    private void markExistingCandidates(
            List<OptimizedCandidate> candidates,
            Map<Integer, String> existingMappings,
            Set<Integer> usedCandidateIndices) {
        for (String mappedText : existingMappings.values()) {
            String normalizedMapped = normalizeForMatch(mappedText);
            if (normalizedMapped.isBlank()) continue;
            int bestIdx = -1;
            double bestScore = 0;
            for (OptimizedCandidate candidate : candidates) {
                if (usedCandidateIndices.contains(candidate.index())) continue;
                double score = normalizedMapped.equals(candidate.normalized())
                    ? 1.0
                    : DocxTextUtils.similarity(normalizedMapped, candidate.normalized());
                if (score > bestScore) {
                    bestScore = score;
                    bestIdx = candidate.index();
                }
            }
            if (bestIdx >= 0 && bestScore >= 0.80) {
                usedCandidateIndices.add(bestIdx);
            }
        }
    }

    private boolean isSafeResumeReplacement(
            String original,
            String normalizedOriginal,
            OptimizedCandidate candidate) {
        int originalLength = Math.max(1, countSignificantForResume(original));
        int candidateLength = Math.max(1, candidate.significantLength());
        double ratio = (double) Math.min(originalLength, candidateLength) / Math.max(originalLength, candidateLength);

        if (isUnsafeResumeSnippetReplacement(original, candidate.text())) {
            return false;
        }

        if (hasSameFieldLabel(original, candidate.text())) {
            if (addsExtraFieldLabels(original, candidate.text())) {
                return false;
            }
            return candidateLength <= Math.max(80, originalLength * 3);
        }

        if (looksLikeDateRangeLine(original) || looksLikeDateRangeLine(candidate.text())) {
            return false;
        }

        if (normalizedOriginal.length() <= 8) {
            return ratio >= 0.55 && candidateLength <= originalLength * 2;
        }

        return ratio >= 0.35
            && candidateLength <= Math.max(40, originalLength * 3)
            && hasMeaningfulOverlap(normalizedOriginal, candidate.normalized());
    }

    private boolean looksLikeDateRangeLine(String text) {
        if (text == null) return false;
        String trimmed = text.trim().toLowerCase();
        if (trimmed.isBlank()) return false;
        return trimmed.matches("^(20xx|19xx|\\d{4}|xx|xxxx)[.\\-/\\s\\u2013\\u2014]*(.*)$");
    }

    private boolean canImproveExistingResumeMapping(
            String original,
            String currentMappedText,
            OptimizedCandidate candidate) {
        if (currentMappedText == null || currentMappedText.isBlank()) return true;
        if (!hasSameFieldLabel(original, candidate.text())) return false;
        String currentLabel = fieldLabel(currentMappedText);
        String candidateLabel = fieldLabel(candidate.text());
        return currentLabel.isBlank() || currentLabel.equals(candidateLabel);
    }

    private boolean shouldReplaceExistingResumeMapping(String currentMappedText, String candidateText) {
        if (currentMappedText == null || currentMappedText.isBlank()) return true;
        String normalizedCurrent = normalizeForMatch(currentMappedText);
        String normalizedCandidate = normalizeForMatch(candidateText);
        if (normalizedCurrent.equals(normalizedCandidate)) return false;
        if (normalizedCandidate.contains(normalizedCurrent) && normalizedCandidate.length() > normalizedCurrent.length()) {
            return true;
        }
        if (hasSameFieldLabel(currentMappedText, candidateText)
            && normalizedCandidate.length() > normalizedCurrent.length() * 1.15) {
            return true;
        }
        return false;
    }

    private double resumeReplacementScore(
            String original,
            String normalizedOriginal,
            OptimizedCandidate candidate) {
        if (hasSameFieldLabel(original, candidate.text())) {
            return 0.95;
        }
        if (normalizedOriginal.contains(candidate.normalized()) || candidate.normalized().contains(normalizedOriginal)) {
            return 0.90;
        }
        double similarity = DocxTextUtils.similarity(normalizedOriginal, candidate.normalized());
        return hasMeaningfulOverlap(normalizedOriginal, candidate.normalized())
            ? Math.max(similarity, 0.50)
            : similarity;
    }

    private double safeResumeThreshold(int normalizedOriginalLength) {
        if (normalizedOriginalLength <= 8) return 0.70;
        if (normalizedOriginalLength <= 20) return 0.55;
        return 0.32;
    }

    private boolean hasSameFieldLabel(String original, String candidate) {
        String originalLabel = fieldLabel(original);
        if (originalLabel.isBlank()) return false;
        return originalLabel.equals(fieldLabel(candidate));
    }

    private boolean addsExtraFieldLabels(String original, String candidate) {
        int originalLabels = countFieldLabels(original);
        int candidateLabels = countFieldLabels(candidate);
        return originalLabels > 0 && candidateLabels > originalLabels;
    }

    /**
     * Snippet-level safety check: only rejects when the replacement would
     * ADD new labels (merge fields) into a text-box paragraph.
     *
     * <p>Unlike {@link #isUnsafeResumeSnippetReplacement}, this does NOT reject
     * replacements where both before and after have the same number of labels.
     * Snippet replacement patches only the matched substring within the textbox,
     * so same-label replacements (e.g. updating a specific field) are safe.</p>
     */
    private boolean snippetWouldDamageTextboxLayout(String before, String after,
                                                     List<ParagraphProfile> profiles, int matchedIdx) {
        if (before == null || after == null) return false;
        ParagraphProfile profile = null;
        for (ParagraphProfile p : profiles) {
            if (p.index() == matchedIdx) { profile = p; break; }
        }
        if (profile == null) return false;

        // Only apply relaxed check for text-box paragraphs;
        // body/table paragraphs use the strict isUnsafeResumeSnippetReplacement.
        String pathStr = profile.path() != null ? profile.path().pathString() : "";
        if (!pathStr.startsWith("txbx[")) {
            return isUnsafeResumeSnippetReplacement(before, after);
        }

        String normBefore = normalizeForMatch(before);
        String normAfter  = normalizeForMatch(after);
        if (normBefore.isBlank() || normAfter.isBlank()) return false;

        String normPara = normalizeForMatch(profile.text());
        double paraCoverage = normPara.length() > 0 ? (double) normBefore.length() / normPara.length() : 0;

        // If before text covers most of the paragraph, this is effectively a
        // full-paragraph replacement. Use the strict check for safety.
        if (paraCoverage > 0.85) {
            return isUnsafeResumeSnippetReplacement(before, after);
        }

        // For true snippet replacements (partial textbox edits), only reject
        // when the after text adds NEW labels that would merge fields.
        int beforePersonal = countKnownLabels(normBefore, PERSONAL_INFO_LABELS);
        int afterPersonal  = countKnownLabels(normAfter, PERSONAL_INFO_LABELS);
        if (beforePersonal > 0 && afterPersonal > beforePersonal) return true;

        int beforeSection = countKnownLabels(normBefore, RESUME_SECTION_LABELS);
        int afterSection  = countKnownLabels(normAfter, RESUME_SECTION_LABELS);
        if (afterSection > beforeSection) return true;

        int beforeField = countFieldLabels(before);
        int afterField  = countFieldLabels(after);
        if (beforeField > 0 && afterField > beforeField) return true;

        return false;
    }

    private boolean isUnsafeResumeSnippetReplacement(String original, String candidate) {
        if (original == null || candidate == null) return false;
        String normalizedOriginal = normalizeForMatch(original);
        String normalizedCandidate = normalizeForMatch(candidate);
        if (normalizedOriginal.isBlank() || normalizedCandidate.isBlank()) return false;

        int originalPersonalLabels = countKnownLabels(normalizedOriginal, PERSONAL_INFO_LABELS);
        int candidatePersonalLabels = countKnownLabels(normalizedCandidate, PERSONAL_INFO_LABELS);
        if (originalPersonalLabels >= 2 && candidatePersonalLabels >= 2) {
            return true;
        }
        if (originalPersonalLabels > 0 && candidatePersonalLabels > originalPersonalLabels) {
            return true;
        }

        int originalSectionLabels = countKnownLabels(normalizedOriginal, RESUME_SECTION_LABELS);
        int candidateSectionLabels = countKnownLabels(normalizedCandidate, RESUME_SECTION_LABELS);
        if (candidateSectionLabels > originalSectionLabels) {
            return true;
        }

        int originalFieldLabels = countFieldLabels(original);
        int candidateFieldLabels = countFieldLabels(candidate);
        return originalFieldLabels > 0 && candidateFieldLabels > originalFieldLabels;
    }

    private int countKnownLabels(String normalizedText, String[] labels) {
        int count = 0;
        for (String label : labels) {
            String normalizedLabel = normalizeForMatch(label);
            if (!normalizedLabel.isBlank() && normalizedText.contains(normalizedLabel)) {
                count++;
            }
        }
        return count;
    }

    private int countFieldLabels(String text) {
        if (text == null || text.isBlank()) return 0;
        int count = 0;
        Matcher matcher = FIELD_LABEL_PATTERN.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String fieldLabel(String text) {
        if (text == null) return "";
        int idx = firstIndexOfAny(text, '：', ':');
        if (idx <= 0 || idx > 12) return "";
        return normalizeForMatch(text.substring(0, idx));
    }

    private int firstIndexOfAny(String text, char first, char second) {
        int a = text.indexOf(first);
        int b = text.indexOf(second);
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }

    private boolean hasMeaningfulOverlap(String normalizedOriginal, String normalizedCandidate) {
        if (normalizedOriginal.length() < 4 || normalizedCandidate.length() < 4) return false;
        int[] windows = {8, 6, 4};
        for (int window : windows) {
            if (normalizedOriginal.length() < window) continue;
            for (int start = 0; start + window <= normalizedOriginal.length(); start++) {
                if (normalizedCandidate.contains(normalizedOriginal.substring(start, start + window))) {
                    return true;
                }
            }
        }
        return false;
    }

    private int countSignificantForResume(String text) {
        if (text == null) return 0;
        return normalizeForMatch(text).length();
    }

    private String truncateForLog(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    /**
     * 检测文本的主要语言。
     * @return 0=mixed/unknown, 1=mostly CJK, 2=mostly Latin
     */
    private static int detectLanguage(String text) {
        if (text == null || text.isBlank()) return 0;
        int cjk = 0, latin = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN
                || (c >= '一' && c <= '鿿')) {
                cjk++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                latin++;
            }
        }
        int total = cjk + latin;
        if (total == 0) return 0;
        double cjkRatio = (double) cjk / total;
        if (cjkRatio > 0.60) return 1;  // mostly CJK
        if (cjkRatio < 0.30) return 2;  // mostly Latin
        return 0; // mixed
    }

    /**
     * 段落匹配失败时的兜底：按章节标题匹配。
     * 在原文和优化文本中找相同的章节标题（如"工作经历"、"教育背景"），
     * 将优化文本中该章节的内容整体写入原文对应章节的第一个段落。
     */
    public Map<Integer, String> buildMappingsBySectionMatch(
            List<ParagraphProfile> profiles, String optimizedText) {
        Map<Integer, String> mappings = new LinkedHashMap<>();
        if (optimizedText == null || optimizedText.isBlank()) return mappings;

        String plain = stripMarkdown(optimizedText);
        String[] optLines = plain.split("\\n");

        // 常见简历章节标题关键词
        String[] sectionKeywords = {
            "教育背景", "教育经历", "学历", "教育",
            "工作经历", "工作经验", "项目经历", "项目经验", "实习经历",
            "技能", "专业技能", "技能特长", "技术栈",
            "自我评价", "自我介绍", "个人简介", "个人总结",
            "求职意向", "求职目标", "职业目标",
            "获奖", "荣誉", "证书", "资格",
            "基本信息", "个人信息", "联系方式",
            "EDUCATION", "EXPERIENCE", "SKILLS", "PROJECTS",
            "SUMMARY", "OBJECTIVE", "CERTIFICATIONS"
        };

        // 找到原文中的章节标题段落
        List<int[]> origSections = new ArrayList<>(); // [profileIndex, sectionStart]
        for (int i = 0; i < profiles.size(); i++) {
            String text = profiles.get(i).text();
            if (text == null) continue;
            String norm = text.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase();
            for (String kw : sectionKeywords) {
                if (norm.contains(kw.toLowerCase().replaceAll("[\\s\\p{P}]", ""))) {
                    origSections.add(new int[]{i, profiles.get(i).index()});
                    break;
                }
            }
        }

        // 找到优化文本中的章节标题行
        List<int[]> optSections = new ArrayList<>(); // [lineIndex, keywordIndex]
        for (int i = 0; i < optLines.length; i++) {
            String lineNorm = optLines[i].replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase();
            for (int ki = 0; ki < sectionKeywords.length; ki++) {
                if (lineNorm.contains(sectionKeywords[ki].toLowerCase().replaceAll("[\\s\\p{P}]", ""))) {
                    optSections.add(new int[]{i, ki});
                    break;
                }
            }
        }

        // 按章节标题匹配
        Set<Integer> usedKeywords = new HashSet<>();
        for (int[] orig : origSections) {
            String origText = profiles.get(orig[0]).text();
            String origNorm = origText.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase();

            for (int[] opt : optSections) {
                if (usedKeywords.contains(opt[1])) continue;
                String kw = sectionKeywords[opt[1]].toLowerCase().replaceAll("[\\s\\p{P}]", "");
                if (origNorm.contains(kw)) {
                    // 找到匹配的章节，提取优化文本中该章节的内容
                    int startLine = opt[0];
                    int endLine = optLines.length;
                    for (int[] next : optSections) {
                        if (next[0] > startLine) { endLine = next[0]; break; }
                    }
                    StringBuilder sectionContent = new StringBuilder();
                    for (int li = startLine; li < endLine; li++) {
                        if (!optLines[li].isBlank()) {
                            if (sectionContent.length() > 0) sectionContent.append("\n");
                            sectionContent.append(optLines[li].trim());
                        }
                    }
                    if (sectionContent.length() > 10) {
                        mappings.put(orig[1], sectionContent.toString());
                        usedKeywords.add(opt[1]);
                    }
                    break;
                }
            }
        }

        if (!mappings.isEmpty()) {
            log.info("章节匹配: {} 个章节匹配成功", mappings.size());
        }
        return mappings;
    }

    static String stripMarkdown(String text) {
        return DocxTextUtils.stripMarkdown(text);
    }
}
