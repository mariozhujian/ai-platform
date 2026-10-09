package cn.mario.aiplatform.controller;


import cn.mario.aiplatform.dto.ChatRequest;
import cn.mario.aiplatform.service.agent.CustomerAgent;
import cn.mario.aiplatform.vo.IntentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description: 消费者售后
 * @author: mario
 * @date: 2026/9/23
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerAgent agent;

    @PostMapping("/chat")
    public IntentResult chat(@RequestBody ChatRequest request) {
        return agent.chat(request.message());
    }

}
