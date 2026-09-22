package cn.mario.aiplatform.dto;


import lombok.Data;

/**
 * @description: TODO
 * @author: mario
 * @date: 9/14/26
 */
@Data
public class KnowledgeBaseDTO {

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
     * 负责人
     */
    private String ownerId;

}
