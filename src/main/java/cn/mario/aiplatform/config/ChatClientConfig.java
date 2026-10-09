package cn.mario.aiplatform.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: ChatClient config
 * @author: mario
 * @date: 9/2/26
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient defaultChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

}
