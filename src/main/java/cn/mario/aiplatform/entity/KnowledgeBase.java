package cn.mario.aiplatform.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @description: 知识库
 * @author: mario
 * @date: 9/14/26
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识库上传者
     */
    private String ownerId;

}
