package cn.mario.aiplatform.parser;


import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: TODO
 * @author: mario
 * @date: 9/3/26
 */
@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public List<Document> parse(Resource resource) {
        // 第一步：PDF -> Document
        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
        return reader.get();
    }
}
