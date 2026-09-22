package cn.mario.aiplatform.service.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 食谱 Agent
 */

public interface RecipeAgent {

    @Agent("Generates a story based on the given topic")
    @UserMessage("""
            
            """)
    @SystemMessage("""
            
            """)
    String recipeAgent();

}
