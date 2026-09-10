package cn.mario.aiplatform.service;

import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

/**
 * @auther: mario
 */
public interface PdfKnowledgeService {

    /**
     * 导入 PDF
     * @param resource
     */
    void importPdf(Resource resource);

    /**
     * 普通一次性问答
     * @param message 问题
     * @return 答案
     */
    String query(String message);

    /**
     * 流式输出
     * @param message 问题
     * @return 答案
     */
    Flux<String> stream(String message);
}
