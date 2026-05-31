package com.mianmiantong.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans a DOCX and produces addressable paragraphs with stable paths.
 *
 * <p>Apache POI handles normal body/table/header/footer paragraphs. Resume
 * templates often put their real content inside text boxes, shapes, WPS drawing
 * containers, or mc:AlternateContent. The DOM fallback reads those raw OOXML
 * paragraphs and exposes them as txbx paths for XML-level patching.</p>
 */
@Slf4j
public class DocxTextLocator {

    static final String DOCX_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    static final String DRAWING_NS = "http://schemas.openxmlformats.org/drawingml/2006/main";

    private static final int DEFAULT_MIN_LENGTH = 4;

    public record LocatorResult(List<ParagraphProfile> profiles, Map<String, DocxPath> textBoxTextToPath) {}

    public LocatorResult locateWithTextBoxMap(byte[] docxBytes) {
        List<ParagraphProfile> profiles = new ArrayList<>();
        Map<String, DocxPath> textBoxTextToPath = new LinkedHashMap<>();

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            int bodyCount = 0;
            int tableCount = 0;
            int headerFooterCount = 0;
            int domCount = 0;
            int skipped = 0;

            List<XWPFParagraph> bodyParas = doc.getParagraphs();
            for (int i = 0; i < bodyParas.size(); i++) {
                XWPFParagraph para = bodyParas.get(i);
                if (isImageOnly(para)) {
                    skipped++;
                    continue;
                }
                ParagraphProfile profile = tryExtract(profiles.size(), para, DocxPath.body(i), DEFAULT_MIN_LENGTH);
                if (profile != null) {
                    profiles.add(profile);
                    bodyCount++;
                }
            }

            List<XWPFTable> tables = doc.getTables();
            for (int ti = 0; ti < tables.size(); ti++) {
                List<XWPFTableRow> rows = tables.get(ti).getRows();
                for (int ri = 0; ri < rows.size(); ri++) {
                    List<XWPFTableCell> cells = rows.get(ri).getTableCells();
                    for (int ci = 0; ci < cells.size(); ci++) {
                        List<XWPFParagraph> cellParas = cells.get(ci).getParagraphs();
                        for (int pi = 0; pi < cellParas.size(); pi++) {
                            XWPFParagraph para = cellParas.get(pi);
                            if (isImageOnly(para)) continue;
                            ParagraphProfile profile = tryExtract(
                                profiles.size(),
                                para,
                                DocxPath.tableCell(ti, ri, ci, pi),
                                2);
                            if (profile != null) {
                                profiles.add(profile);
                                tableCount++;
                            }
                        }
                    }
                }
            }

            List<XWPFHeader> headers = doc.getHeaderList();
            for (int hi = 0; hi < headers.size(); hi++) {
                List<XWPFParagraph> hParas = headers.get(hi).getParagraphs();
                for (int pi = 0; pi < hParas.size(); pi++) {
                    XWPFParagraph para = hParas.get(pi);
                    if (isImageOnly(para)) continue;
                    ParagraphProfile profile = tryExtract(profiles.size(), para, DocxPath.header(hi, pi), 2);
                    if (profile != null) {
                        profiles.add(profile);
                        headerFooterCount++;
                    }
                }
            }

            List<XWPFFooter> footers = doc.getFooterList();
            for (int fi = 0; fi < footers.size(); fi++) {
                List<XWPFParagraph> fParas = footers.get(fi).getParagraphs();
                for (int pi = 0; pi < fParas.size(); pi++) {
                    XWPFParagraph para = fParas.get(pi);
                    if (isImageOnly(para)) continue;
                    ParagraphProfile profile = tryExtract(profiles.size(), para, DocxPath.footer(fi, pi), 2);
                    if (profile != null) {
                        profiles.add(profile);
                        headerFooterCount++;
                    }
                }
            }

            CTBody body = doc.getDocument().getBody();
            if (body != null) {
                Set<String> existingTexts = new HashSet<>();
                for (ParagraphProfile profile : profiles) {
                    String normalized = DocxTextUtils.normalize(profile.text());
                    if (!normalized.isBlank()) existingTexts.add(normalized);
                }

                List<ParagraphProfile> domProfiles = findDomParagraphs(body, profiles.size(), existingTexts);
                for (ParagraphProfile profile : domProfiles) {
                    profiles.add(profile);
                    textBoxTextToPath.put(profile.text(), profile.path());
                    domCount++;
                }
            }

            log.info("DOCX locate complete: total={}, body={}, table={}, hf={}, dom={}, skipped={}",
                profiles.size(), bodyCount, tableCount, headerFooterCount, domCount, skipped);
        } catch (Exception e) {
            log.error("DOCX locate failed", e);
            throw new RuntimeException("DOCX locate failed: " + e.getMessage(), e);
        }

        return new LocatorResult(profiles, textBoxTextToPath);
    }

    public List<ParagraphProfile> locate(byte[] docxBytes) {
        return locateWithTextBoxMap(docxBytes).profiles();
    }

    private List<ParagraphProfile> findDomParagraphs(
        CTBody body,
        int startIndex,
        Set<String> existingTexts) {
        List<ParagraphProfile> result = new ArrayList<>();
        String bodyXml = body.xmlText();

        try {
            DocumentBuilderFactory factory = newSecureDocumentBuilderFactory();
            Document xmlDoc = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(bodyXml.getBytes(StandardCharsets.UTF_8)));

            NodeList pNodes = xmlDoc.getElementsByTagNameNS(DOCX_NS, "p");
            int readable = 0;
            int txbxAncestor = 0;
            int containerSkipped = 0;
            log.info("DOM scan: w:p={}, w:t={}, a:t={}, txbxContent={}",
                pNodes.getLength(),
                countNodes(xmlDoc, DOCX_NS, "t"),
                countNodes(xmlDoc, DRAWING_NS, "t"),
                countNodes(xmlDoc, DOCX_NS, "txbxContent"));

            for (int i = 0; i < pNodes.getLength(); i++) {
                Node pNode = pNodes.item(i);
                boolean insideTextBox = hasAncestor(pNode, DOCX_NS, "txbxContent");
                if (insideTextBox) txbxAncestor++;
                if (!insideTextBox) continue;
                if (hasDescendant(pNode, DOCX_NS, "p")) {
                    containerSkipped++;
                    continue;
                }

                String text = extractTextFromParagraphNode(pNode).trim();
                if (text.isBlank()) continue;
                readable++;

                String normalized = DocxTextUtils.normalize(text);
                if (normalized.isBlank() || existingTexts.contains(normalized)) continue;
                if (countSignificantChars(text) < 2) continue;

                DocxPath path = DocxPath.textBox(result.size());
                ParagraphProfile profile = ParagraphProfile.fromRaw(startIndex + result.size(), text, path);
                result.add(profile);
                existingTexts.add(normalized);

                log.info("  DOM paragraph[{}]: path={}, text='{}'",
                    result.size() - 1,
                    path.pathString(),
                    text.length() > 60 ? text.substring(0, 60) + "..." : text);
            }

            log.info("DOM scan summary: readable={}, txbxAncestor={}, containerSkipped={}, accepted={}",
                readable, txbxAncestor, containerSkipped, result.size());
        } catch (Exception e) {
            log.error("DOM paragraph scan failed", e);
        }

        return result;
    }

    private DocumentBuilderFactory newSecureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private int countNodes(Document document, String namespace, String localName) {
        return document.getElementsByTagNameNS(namespace, localName).getLength();
    }

    private boolean hasAncestor(Node node, String ns, String localName) {
        Node parent = node.getParentNode();
        while (parent != null) {
            if (ns.equals(parent.getNamespaceURI()) && localName.equals(parent.getLocalName())) {
                return true;
            }
            parent = parent.getParentNode();
        }
        return false;
    }

    private boolean hasDescendant(Node node, String ns, String localName) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (ns.equals(child.getNamespaceURI()) && localName.equals(child.getLocalName())) {
                return true;
            }
            if (hasDescendant(child, ns, localName)) {
                return true;
            }
        }
        return false;
    }

    static String extractTextFromParagraphNode(Node pNode) {
        StringBuilder sb = new StringBuilder();
        appendTextNodes(pNode, sb);
        return sb.toString();
    }

    private static void appendTextNodes(Node node, StringBuilder sb) {
        if (node == null) return;

        String namespace = node.getNamespaceURI();
        String localName = node.getLocalName();
        if ("t".equals(localName) && (DOCX_NS.equals(namespace) || DRAWING_NS.equals(namespace))) {
            sb.append(node.getTextContent());
            return;
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            appendTextNodes(children.item(i), sb);
        }
    }

    private boolean isImageOnly(XWPFParagraph para) {
        boolean hasImage = false;
        boolean hasText = false;
        for (XWPFRun run : para.getRuns()) {
            String t = run.getText(0);
            if (t != null && !t.isBlank()) hasText = true;
            if (!run.getEmbeddedPictures().isEmpty()) hasImage = true;
        }
        return hasImage && !hasText;
    }

    private ParagraphProfile tryExtract(int index, XWPFParagraph para, DocxPath path, int minLength) {
        String text = extractFullText(para);
        if (text == null || text.isBlank()) return null;
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
        if (countSignificantChars(text) < minLength) return null;
        return ParagraphProfile.from(index, para, path);
    }

    private int countSignificantChars(String text) {
        String stripped = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
            .replaceAll("[\\s\\p{P}\\p{S}0-9]", "");
        return stripped.length();
    }

    private String extractFullText(XWPFParagraph para) {
        String text = para.getText();
        if (text != null && !text.isBlank()) return text;

        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : para.getRuns()) {
            String rt = run.getText(0);
            if (rt != null) sb.append(rt);
        }
        return sb.toString();
    }
}
