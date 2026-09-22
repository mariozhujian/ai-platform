package cn.mario.aiplatform.mapper;

import cn.mario.aiplatform.entity.KnowledgeImport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface KnowledgeImportMapper extends BaseMapper<KnowledgeImport> {

    @Insert("""
    INSERT INTO knowledge_pdf_import (file_hash, filename)
    VALUES (#{hash}, #{filename})
    ON CONFLICT (file_hash) DO NOTHING
    """)
    int insertIfAbsent( @Param("hash") String hash,
                        @Param("filename") String filename);

}
