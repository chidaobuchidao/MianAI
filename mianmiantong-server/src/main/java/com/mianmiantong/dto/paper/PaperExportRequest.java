package com.mianmiantong.dto.paper;

import lombok.Data;
import java.util.List;

@Data
public class PaperExportRequest {
    /** @deprecated 不再使用服务器端缓存，文件由前端提交 */
    private String uploadId;
    /** 导出文件名（不含扩展名） */
    private String fileName;
    /** 改写后的段落映射：index 对应 ParagraphProfile.index */
    private List<ParagraphMapping> paragraphs;

    @Data
    public static class ParagraphMapping {
        private int index;
        private String text;
    }
}
