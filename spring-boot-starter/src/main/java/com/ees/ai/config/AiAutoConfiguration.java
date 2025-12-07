package com.ees.ai.config;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiSessionService;
import com.ees.ai.core.AiToolRegistry;
import com.ees.ai.core.DefaultAiAgentService;
import com.ees.ai.core.DefaultAiToolRegistry;
import com.ees.ai.core.InMemoryAiSessionService;
import com.ees.ai.support.NoOpChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties(AiAgentProperties.class)
public class AiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AiToolRegistry aiToolRegistry() {
        return new DefaultAiToolRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AiSessionService aiSessionService() {
        return new InMemoryAiSessionService();
    }

    @Bean
    @ConditionalOnMissingBean
    public AiAgentService aiAgentService(ChatModel chatModel,
                                         StreamingChatModel streamingChatModel,
                                         AiSessionService aiSessionService,
                                         AiToolRegistry aiToolRegistry,
                                         AiAgentProperties aiAgentProperties) {
        return new DefaultAiAgentService(chatModel, streamingChatModel, aiSessionService, aiToolRegistry, aiAgentProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel noopChatModel() {
        return new NoOpChatModel();
    }

    @Bean
    @ConditionalOnMissingBean(StreamingChatModel.class)
    public StreamingChatModel noopStreamingChatModel(NoOpChatModel delegate) {
        return delegate;
    }
}
