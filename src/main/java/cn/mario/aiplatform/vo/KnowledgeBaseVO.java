package cn.mario.aiplatform.vo;


import lombok.Data;

/**
 * @description: TODO
 * @author: mario
 * @date: 9/14/26
 */
@Data
public class KnowledgeBaseVO {

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
