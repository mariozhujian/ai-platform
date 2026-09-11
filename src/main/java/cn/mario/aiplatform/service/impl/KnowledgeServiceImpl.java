package cn.mario.aiplatform.service.impl;


import cn.mario.aiplatform.chunk.KnowledgeChunker;
import cn.mario.aiplatform.exception.BizException;
import cn.mario.aiplatform.parser.KnowledgeDocumentParser;
import cn.mario.aiplatform.service.KnowledgeService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * @description: TODO
 * @author: mario
 * @date: 9/11/26
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private KnowledgeDocumentParser knowledgeDocumentParser;
    @Autowired
    private KnowledgeChunker knowledgeChunker;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private VectorStore vectorStore;

    @Override
    public void importDocument(Resource resource) {
        String hash = fileHash(resource);

        Boolean exists = jdbcTemplate.queryForObject(
                """
                    SELECT EXISTS (
                        SELECT 1 FROM knowledge_pdf_import WHERE file_hash = ?
                    )
                    """, Boolean.class, hash);

        if (Boolean.TRUE.equals(exists)) {
            throw new BizException(409, "该文件已上传，请勿重复上传");
        }

        // 解析和 OCR 不占用数据库事务
        List<Document> documents = knowledgeDocumentParser.parse(resource);
        List<Document> chunks = knowledgeChunker.chunk(documents);
        if (chunks.isEmpty()) {
            throw new BizException("文件未解析出可导入的文字");
        }
        chunks.forEach(chunk -> chunk.getMetadata().put("file_hash", hash));

        // 事务内执行
        transactionTemplate.executeWithoutResult(status -> {
            // 提前查询只是优化；最终仍由唯一键处理并发重复上传
            int inserted = jdbcTemplate.update("""
                                
                                INSERT INTO knowledge_pdf_import (file_hash, filename)
                                VALUES (?, ?)
                                ON CONFLICT (file_hash) DO NOTHING
                                """, hash, resource.getFilename());

            if (inserted == 0) {
                throw new BizException(409, "该文件已上传，请勿重复上传");
            }

        });
        vectorStore.add(chunks);
    }

    /**
     * 读取文件内容并计算 SHA-256 哈希，用于识别重复上传的 PDF。
     *
     * @param resource PDF 文件资源
     * @return 文件内容的 SHA-256 十六进制字符串
     */
    private String fileHash(Resource resource) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new DigestInputStream(resource.getInputStream(), digest)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new BizException("读取 PDF 文件失败，请重新上传");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
