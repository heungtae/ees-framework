package com.ees.ai.config;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiSessionService;
import com.ees.ai.core.AiToolRegistry;
import com.ees.ai.core.DefaultAiAgentService;
import com.ees.ai.core.DefaultAiToolRegistry;
import com.ees.ai.core.InMemoryAiSessionService;
import com.ees.ai.support.NoOpChatModel;
import com.ees.ai.mcp.DefaultMcpClient;
import com.ees.ai.mcp.McpClient;
import com.ees.ai.mcp.McpProperties;
import com.ees.ai.mcp.McpToolBridge;
import com.ees.ai.mcp.LoggingMcpAuditService;
import com.ees.ai.mcp.McpAuditService;
import com.ees.ai.mcp.RestMcpClient;
import java.util.List;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties({AiAgentProperties.class, McpProperties.class})
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
                                         AiAgentProperties aiAgentProperties,
                                         List<ToolCallback> toolCallbacks) {
        return new DefaultAiAgentService(chatModel, streamingChatModel, aiSessionService, aiToolRegistry, aiAgentProperties, toolCallbacks);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpClient mcpClient() {
        return new DefaultMcpClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public McpAuditService mcpAuditService() {
        return new LoggingMcpAuditService();
    }

    @Bean(name = "mcpWebClient")
    @ConditionalOnMissingBean(name = "mcpWebClient")
    @ConditionalOnProperty(prefix = "ees.mcp", name = "base-url")
    public WebClient mcpWebClient(McpProperties properties) {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(java.time.Duration.ofMillis(properties.getTimeoutMillis()));
        return WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "restMcpClient")
    @ConditionalOnProperty(prefix = "ees.mcp", name = "base-url")
    public McpClient restMcpClient(WebClient mcpWebClient) {
        return new RestMcpClient(mcpWebClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpToolBridge mcpToolBridge(McpClient mcpClient, AiToolRegistry aiToolRegistry, McpAuditService auditService) {
        return new McpToolBridge(mcpClient, aiToolRegistry, auditService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mcpToolCallbacks")
    public List<ToolCallback> mcpToolCallbacks(McpToolBridge bridge) {
        return bridge.toolCallbacks();
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
