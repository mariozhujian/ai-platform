package cn.mario.aiplatform.parser;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 文档解析器
 */
public interface DocumentParser {

    List<Document> parse(Resource resource);

}
