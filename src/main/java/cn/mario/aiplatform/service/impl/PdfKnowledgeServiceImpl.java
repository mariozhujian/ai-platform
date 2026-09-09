package cn.mario.aiplatform.service.impl;

import cn.mario.aiplatform.chunk.KnowledgeChunker;
import cn.mario.aiplatform.parser.PdfDocumentParser;
import cn.mario.aiplatform.service.PdfKnowledgeService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @auther: mario
 */
@Service
public class PdfKnowledgeServiceImpl implements PdfKnowledgeService {

    @Autowired
    private PdfDocumentParser pdfDocumentParser;
    @Autowired
    private KnowledgeChunker knowledgeChunker;
    @Autowired
    private VectorStore vectorStore;

    @Override
    public void importPdf(Resource resource) {
        // 1.PDF解析
        List<Document> documents = pdfDocumentParser.parse(resource);

        // 2.切Chunk
        List<Document> chunks = knowledgeChunker.chunk(documents);

        // 3.向量化保存,Add the documents to PGVector
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
    }
}
