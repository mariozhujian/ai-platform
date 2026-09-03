package cn.mario.aiplatform.service.impl;

import cn.mario.aiplatform.chunk.KnowledgeChunker;
import cn.mario.aiplatform.parser.PdfDocumentParser;
import cn.mario.aiplatform.service.KnowledgeImportService;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @auther: mario
 */
@Service
public class KnowledgeImportServiceImpl implements KnowledgeImportService {

    @Autowired
    private PdfDocumentParser pdfDocumentParser;
    @Autowired
    private KnowledgeChunker knowledgeChunker;

    @Override
    public void importPdf(Resource resource) {
        // 1.PDF解析
        List<Document> documents = pdfDocumentParser.parse(resource);


        // 2.切Chunk
        List<Document> chunks = knowledgeChunker.chunk(documents);

        // 3.向量化保存
    }
}
