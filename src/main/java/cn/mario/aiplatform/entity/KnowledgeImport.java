package cn.mario.aiplatform.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * @description: TODO
 * @author: mario
 * @date: 9/14/26
 */
@Data
@TableName("knowledge_import")
public class KnowledgeImport {

    /**
     * 文件 id
     */
    @TableId(value = "file_hash", type = IdType.INPUT)
    private String fileHash;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;
}
