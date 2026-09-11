package cn.mario.aiplatform.parser;


import cn.mario.aiplatform.exception.BizException;
import cn.mario.aiplatform.service.OcrService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @description: 文件解析
 * @author: mario
 * @date: 9/11/26
 */
@Component
public class KnowledgeDocumentParser implements DocumentParser {

    private static final int MAX_BYTES = 20 * 1024 * 1024;

    private static final Set<String> SUPPORTED = Set.of("pdf", "doc", "docx", "txt", "md",
            "markdown", "png", "jpg", "jpeg");

    @Value("${knowledge.ocr.max-pages:30}")
    private int maxPages;

    @Value("${knowledge.ocr.dpi:180}")
    private float dpi;

    @Value("${knowledge.ocr.mode:auto}")
    private String ocrMode;

    @Autowired
    private OcrService ocrService;


    @Override
    public List<Document> parse(Resource resource) {
        String filename = Optional.ofNullable(resource.getFilename())
                .orElse("unknown");

        String extension = filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);

        if (!SUPPORTED.contains(extension)) {
            throw new BizException("不支持的文件类型：" + extension);
        }

        try{
            byte[] bytes;
            try(var input = resource.getInputStream()) {
                bytes = input.readNBytes(MAX_BYTES + 1);
            }

            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw new BizException("文件不能为空且不能超过 20MB");
            }

            List<Document> documents = switch (extension) {
                case "txt", "md", "markdown" ->
                    List.of(new Document(decodeUtf8(bytes)));
                case "doc", "docx" -> readWord(bytes, filename, extension);
                case "pdf" -> readPdf(resource);
                case "png", "jpg", "jpeg" -> readImage(bytes, extension);
                default -> throw new BizException("不支持的文件类型");
            };

            documents.forEach(document -> {
                var metadata = document.getMetadata();
                metadata.put("filename", filename);
                metadata.put("file_type", extension);
                metadata.putIfAbsent("parse_method", "text");
            });

            return documents;
        } catch (IOException e) {
            throw new BizException("文件读取或解析失败，请检查文件是否损坏");
        }
    }

    private List<Document> readImage(byte[] bytes, String extension) throws IOException {
        Path image = Files.createTempFile("knowledge-image-", "." + extension);
        try {
            Files.write(image, bytes);

            // 只读取图片头中的尺寸，避免先完整解码超大图片
            try (var input = ImageIO.createImageInputStream(image.toFile())) {
                if (input == null) {
                    throw new BizException("无法读取图片");
                }
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    throw new BizException("文件不是有效图片");
                }

                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!format.equalsIgnoreCase("png")
                            && !format.equalsIgnoreCase("jpeg")) {
                        throw new BizException("仅支持 PNG/JPEG 图片");
                    }

                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels > 20_000_000) {
                        throw new BizException("图片不能超过 2000 万像素");
                    }
                } finally {
                    reader.dispose();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("parse_method", "ocr");

            return List.of(new Document(ocrService.recognize(image), metadata));
        } finally {
            Files.deleteIfExists(image);
        }

    }

    private List<Document> readPdf(Resource resource) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        return reader.get();

//        if (!new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII).equals("%PDF-")) {
//                throw new BizException("文件内容不是 PDF");
//        }
//        if (!Set.of("auto", "always").contains(ocrMode)) {
//            throw new BizException("knowledge.ocr.mode 只能为 auto 或 always");
//        }
//
//        try (PDDocument pdf = Loader.loadPDF(bytes)) {
//            if (pdf.getNumberOfPages() > maxPages) {
//                 throw new BizException("PDF 不能超过 " + maxPages + " 页");
//            }
//
//            List<Document> documents = new ArrayList<>();
//            PDFTextStripper stripper = new PDFTextStripper();
//            stripper.setSortByPosition(true);
//            PDFRenderer renderer = new PDFRenderer(pdf);
//            for (int index = 0; index < pdf.getNumberOfPages(); index++) {
//                stripper.setStartPage(index + 1);
//                stripper.setEndPage(index + 1);
//
//                String text = stripper.getText(pdf);
//                boolean useOcr = ocrMode.equals("always") || needsOcr(text);
//
//                if (useOcr) {
//                    var page = pdf.getPage(index);
//                    var box = page.getCropBox();
//                    double scale = dpi / 72.0 * page.getUserUnit();
//                    double pixels = box.getWidth() * box.getHeight()
//                            * scale * scale;
//
//                    if (dpi <= 0 || !Double.isFinite(pixels)
//                            || pixels > 20_000_000) {
//                        throw new BizException("PDF 页面过大或 OCR DPI 配置无效");
//                    }
//
//                    Path image = Files.createTempFile("knowledge-page-", ".png");
//                    try {
//                        var rendered = renderer.renderImageWithDPI(index, dpi);
//                        try {
//                            ImageIO.write(rendered, "png", image.toFile());
//                        } finally {
//                            rendered.flush();
//                        }
//                        text = ocrService.recognize(image);
//                    } finally {
//                        Files.deleteIfExists(image);
//                    }
//                }
//
//                Map<String, Object> metadata = new HashMap<>();
//                metadata.put("page_number", index + 1);
//                metadata.put("parse_method", useOcr ? "ocr" : "text");
//                documents.add(new Document(text, metadata));
//            }
//        } catch (Exception e) {
//
//        }
    }

    private boolean needsOcr(String text) {
        // 复杂混合页用 always，后续需要精确识别图片区域时再增加版面分析。
        return text == null || text.codePoints()
                .filter(Character::isLetterOrDigit)
                .limit(20).count() < 20
                || text.contains("\uFFFD");
    }

    /**
     * 读取 word 文档
     * @param bytes
     * @param fileName
     * @param extension
     * @return
     */
    private List<Document> readWord(byte[] bytes, String fileName, String extension) {
        // 容器签名初检；具体内容交给 Word 解析器验证
        String signature = HexFormat.of().formatHex(Arrays.copyOf(bytes, Math.min(bytes.length, 8)));

        boolean valid = extension.equals("doc") ? signature.startsWith("d0cf11e0a1b11ae1") : signature.startsWith("504b0304");
        if (!valid) {
            throw new BizException("Word 文件内容与扩展名不匹配");
        }

        Resource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        try {
            return new TikaDocumentReader(resource).get();
        } catch (RuntimeException e) {
            throw new BizException("Word 解析失败，请检查文件是否损坏或加密");
        }
    }

    private String decodeUtf8(byte[] bytes) {
        // 默认严格 UTF-8；不静默将解码错误替换为乱码。
        String text = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
        // `\uFEFF` 就是文件头部一个看不见的标记字符（BOM），Windows 记事本保存 UTF-8 文件会自动加上；
        // 大部分编程语言、JSON、脚本都不喜欢这个字符，建议删掉。
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }


}
