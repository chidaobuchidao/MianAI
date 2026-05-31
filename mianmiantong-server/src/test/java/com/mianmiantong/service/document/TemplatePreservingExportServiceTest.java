package com.mianmiantong.service.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatePreservingExportServiceTest {

    private final TemplatePreservingExportService service = new TemplatePreservingExportService();

    @Test
    @DisplayName("resume supplement rejects compressed personal info textbox even with same labels")
    void resumeSupplementRejectsCompressedPersonalInfoTextboxWithSameLabels() {
        String original = """
            出生年月：1999.8.3              籍贯：湖南               民族：汉
            政治面貌：中共党员              学历：本科               手机：18800000000
            现 居 地：广东深圳              工作年限：应届生         邮箱：1234@xx.com
            """.strip();
        String compressed = """
            安可儿 求职意向：产品助理（需求分析/原型设计方向）
            出生年月：1999.8.3 籍贯：湖南 民族：汉
            政治面貌：中共党员学历：本科手机：18800000000现居地：广东深圳工作年限：应届生邮箱：1234@xx.com
            """.strip();
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(20, original, DocxPath.textBox(20))
        );

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(
            profiles,
            compressed,
            Map.of());

        assertThat(mappings).isEmpty();
    }

    @Test
    @DisplayName("highlight snippets reject compressed personal info textbox even with same labels")
    void highlightSnippetsRejectCompressedPersonalInfoTextboxWithSameLabels() {
        String original = """
            出生年月：1999.8.3              籍贯：湖南               民族：汉
            政治面貌：中共党员              学历：本科               手机：18800000000
            现 居 地：广东深圳              工作年限：应届生         邮箱：1234@xx.com
            """.strip();
        String compressed = """
            出生年月：1999.8.3 籍贯：湖南 民族：汉
            政治面貌：中共党员学历：本科手机：18800000000现居地：广东深圳工作年限：应届生邮箱：1234@xx.com
            """.strip();
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(20, original, DocxPath.textBox(20))
        );
        List<Map<String, Object>> highlights = List.of(
            Map.of("before", original, "after", compressed)
        );

        Map<Integer, String[]> mappings = service.buildSnippetMappingsFromHighlights(profiles, highlights);

        assertThat(mappings).isEmpty();
    }

    @Test
    @DisplayName("resume supplement fills omitted optimized lines without mapping merged sections")
    void resumeSupplementFillsOmittedOptimizedLines() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(0, "求职意向：计算机类岗位", DocxPath.body(0)),
            ParagraphProfile.fromRaw(1, "根据主席团安排开展工作并定期向主席团汇报工作进展状况；", DocxPath.body(1)),
            ParagraphProfile.fromRaw(2, "组织部网站建设及相关工作，制定管理办法,利用校园网、校报等对本系团学会进行宣传；", DocxPath.body(2)),
            ParagraphProfile.fromRaw(3, "个人技能/Skills", DocxPath.body(3))
        );
        Map<Integer, String> existing = new LinkedHashMap<>();
        existing.put(1, "根据主席团安排分解任务，制定周度计划并定期汇报项目进度，确保里程碑按时达成；");

        String optimizedText = """
            求职意向：项目助理实习

            根据主席团安排分解任务，制定周度计划并定期汇报项目进度，确保里程碑按时达成；

            组织部门网站建设项目，撰写需求文档与管理规范，协调设计、开发与宣传资源，保障项目顺利上线；

            个人技能/Skills
            专业技能：计算机三级证书，熟悉Office、WPS等办公软件
            """;

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(profiles, optimizedText, existing);

        assertThat(mappings)
            .containsEntry(0, "求职意向：项目助理实习")
            .containsEntry(2, "组织部门网站建设项目，撰写需求文档与管理规范，协调设计、开发与宣传资源，保障项目顺利上线；");
        assertThat(mappings).doesNotContainKey(1);
        assertThat(mappings).doesNotContainKey(3);
    }

    @Test
    @DisplayName("resume supplement rejects one-line merged resume sections")
    void resumeSupplementRejectsMergedSections() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(0, "专业技能：熟悉操作系统，编程，计算机网络技术，网页制作", DocxPath.body(0))
        );
        String optimizedText = """
            个人技能/Skills 专业技能：熟悉Linux基础、计算机网络与网页制作 在校经历/Experience 根据主席团安排制定工作计划
            """;

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(
            profiles,
            optimizedText,
            Map.of());

        assertThat(mappings).isEmpty();
    }

    @Test
    @DisplayName("resume supplement keeps labeled course line even when optimized text adds backend terms")
    void resumeSupplementKeepsLabeledCourseLine() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(
                13,
                "主修课程：C语言程序设计，JAVA程序设计，计算机基础，网页设计与制作，数据结构，数据库原理与应用，网络操作系统及应用，数据库系统等。",
                DocxPath.textBox(13))
        );
        String optimizedText = """
            教育背景/Educational
            20xx.09-20xx.06                                         xx大学                                        计算机网络（本科）
            主修课程：C语言程序设计、Java程序设计、计算机基础、网页设计与制作、数据结构、数据库原理与应用、网络操作系统、Spring Boot入门、MySQL数据库等。
            """;

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(
            profiles,
            optimizedText,
            Map.of());

        assertThat(mappings).containsEntry(
            13,
            "主修课程：C语言程序设计、Java程序设计、计算机基础、网页设计与制作、数据结构、数据库原理与应用、网络操作系统、Spring Boot入门、MySQL数据库等。");
    }

    @Test
    @DisplayName("resume supplement can replace incomplete highlight mapping for labeled course line")
    void resumeSupplementReplacesIncompleteHighlightMapping() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(
                13,
                "主修课程：C语言程序设计，JAVA程序设计，计算机基础，网页设计与制作，数据结构，数据库原理与应用，网络操作系统及应用，数据库系统等。",
                DocxPath.textBox(13))
        );
        Map<Integer, String> existing = new LinkedHashMap<>();
        existing.put(13, "主修课程：C语言程序设计、Java程序设计、计算机基础、网页设计与制作、数据结构、数据库原理与应用。");

        String optimizedText = """
            主修课程：C语言程序设计、Java程序设计、计算机基础、网页设计与制作、数据结构、数据库原理与应用、网络操作系统、Spring Boot入门、MySQL数据库等。
            """;

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(
            profiles,
            optimizedText,
            existing);

        assertThat(mappings).containsEntry(
            13,
            "主修课程：C语言程序设计、Java程序设计、计算机基础、网页设计与制作、数据结构、数据库原理与应用、网络操作系统、Spring Boot入门、MySQL数据库等。");
    }

    @Test
    @DisplayName("resume supplement rejects compact field rows merged into one textbox")
    void resumeSupplementRejectsMergedCompactFieldRows() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(
                20,
                "status: active              degree: bachelor               phone: 18800000000",
                DocxPath.textBox(20))
        );
        String optimizedText = """
            status: active degree: bachelor phone: 18800000000 city: Shenzhen years: graduate email: 1234@example.com
            """;

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(
            profiles,
            optimizedText,
            Map.of());

        assertThat(mappings).isEmpty();
    }

    @Test
    @DisplayName("resume supplement rejects personal info row merged with following fields")
    void resumeSupplementRejectsPersonalInfoRowMergedWithFollowingFields() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(
                20,
                "政治面貌：中共党员              学历：本科               手机：18800000000",
                DocxPath.textBox(20))
        );
        String optimizedText = """
            政治面貌：中共党员学历：本科手机：18800000000现居地：广东深圳工作年限：应届生邮箱：1234@xx.com
            """;

        Map<Integer, String> mappings = service.buildSafeResumeSupplementMappings(
            profiles,
            optimizedText,
            Map.of());

        assertThat(mappings).isEmpty();
    }

    @Test
    @DisplayName("highlight snippets reject name merged with objective")
    void highlightSnippetsRejectNameMergedWithObjective() {
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(18, "安可儿", DocxPath.textBox(18))
        );
        List<Map<String, Object>> highlights = List.of(
            Map.of(
                "before", "安可儿",
                "after", "安可儿 求职意向：活动策划师")
        );

        Map<Integer, String[]> mappings = service.buildSnippetMappingsFromHighlights(profiles, highlights);

        assertThat(mappings).isEmpty();
    }

    @Test
    @DisplayName("objective snippet updates job intention inside protected personal info textbox")
    void objectiveSnippetUpdatesJobIntentionInsidePersonalInfoTextbox() {
        String original = """
            安可儿       求职意向：活动策划师
            出生年月：1999.8.3              籍贯：湖南               民族：汉
            政治面貌：中共党员              学历：本科               手机：18800000000
            现 居 地：广东深圳              工作年限：应届生         邮箱：1234@xx.com
            """.strip();
        String optimized = """
            安可儿       求职意向：产品经理
            出生年月：1999.8.3 籍贯：湖南 民族：汉
            政治面貌：中共党员学历：本科手机：18800000000现居地：广东深圳工作年限：应届生邮箱：1234@xx.com
            """.strip();
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(20, original, DocxPath.textBox(20))
        );

        Map<Integer, String[]> mappings = service.buildObjectiveSnippetMappings(profiles, optimized);

        assertThat(mappings).containsOnlyKeys(20);
        assertThat(mappings.get(20)).containsExactly("求职意向：活动策划师", "求职意向：产品经理");
    }

    @Test
    @DisplayName("objective snippet trims following personal info labels")
    void objectiveSnippetTrimsFollowingPersonalInfoLabels() {
        String original = "安可儿       求职意向：活动策划师";
        String optimized = "安可儿 求职意向：产品经理 出生年月：1999.8.3 籍贯：湖南";
        List<ParagraphProfile> profiles = List.of(
            ParagraphProfile.fromRaw(20, original, DocxPath.textBox(20))
        );

        Map<Integer, String[]> mappings = service.buildObjectiveSnippetMappings(profiles, optimized);

        assertThat(mappings).containsOnlyKeys(20);
        assertThat(mappings.get(20)).containsExactly("求职意向：活动策划师", "求职意向：产品经理");
    }
}
