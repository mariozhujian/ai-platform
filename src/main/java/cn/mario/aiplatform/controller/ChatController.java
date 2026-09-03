package cn.mario.aiplatform.controller;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @description: 实现 Spring AI + DeepSeek chat
 * @author: mario
 * @date: 9/2/26
 */
@RestController
@RequestMapping("/ai")
public class ChatController {

    /**
     * Spring AI 提供 Spring Boot 自动配置功能，
     * 它会创建一个 ChatClient.Builder bean 原型供您注入到类中
     */
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public String generation(String message) {
//        return this.chatClient.prompt()
//                .user(message)
//                .call()
//                .content();

        ChatResponse chatResponse =  this.chatClient.prompt()
                .system(s -> s.text("你是一个营养健康师"))
                .user(message)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamGeneration(String message) {
//        return this.chatClient.prompt()
//                .user(message)
//                .call()
//                .chatResponse()
////                .content();
        return chatClient.prompt()
                .system("你是一个简洁的技术高手")
                .user(message)
                .stream()    // 关键：.stream() 而非 .call()
                .content();  // Flux<String>
    }

}
