package com.mianmiantong.service.document;

/**
 * A single text patch targeting a paragraph identified by its {@link DocxPath}.
 *
 * @param path   stable path to the paragraph within the DOCX
 * @param before original text for verification before applying the patch
 * @param after  replacement text
 */
public record DocxPatch(DocxPath path, String before, String after) {
}
