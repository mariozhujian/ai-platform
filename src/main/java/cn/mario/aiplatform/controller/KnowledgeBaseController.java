package cn.mario.aiplatform.controller;


import cn.mario.aiplatform.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 知识库
 * @author: mario
 * @date: 9/14/26
 */
@RestController
@RequestMapping("/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;



}
