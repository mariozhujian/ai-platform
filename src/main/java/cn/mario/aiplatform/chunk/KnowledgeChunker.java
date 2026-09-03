package cn.mario.aiplatform.chunk;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 切片
 * @auther: mario
 */
@Component
public class KnowledgeChunker {

    public List<Document> chunk(List<Document> documents) {
        // 标题切分

        // 段落切分

        // TokenTextSplitter

        // metadata 补充

        return documents;
    }

}
