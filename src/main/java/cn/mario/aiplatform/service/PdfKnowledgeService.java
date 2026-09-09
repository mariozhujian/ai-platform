package cn.mario.aiplatform.service;

import org.springframework.core.io.Resource;

/**
 * @auther: mario
 */
public interface PdfKnowledgeService {

    void importPdf(Resource resource);

}
