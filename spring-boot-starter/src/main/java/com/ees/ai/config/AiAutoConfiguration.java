package com.ees.ai.config;

import com.ees.ai.core.AiAgentService;
import com.ees.ai.core.AiRateLimiter;
import com.ees.ai.core.AiSessionService;
import com.ees.ai.core.AiToolRegistry;
import com.ees.ai.core.DefaultAiAgentService;
import com.ees.ai.core.DefaultAiToolRegistry;
import com.ees.ai.core.InMemoryAiSessionService;
import com.ees.ai.core.InMemoryAiRateLimiter;
import com.ees.ai.core.MetadataStoreAiSessionService;
import com.ees.ai.support.NoOpChatModel;
import com.ees.ai.control.ControlAuditService;
import com.ees.ai.control.ControlClient;
import com.ees.ai.control.ControlFacade;
import com.ees.ai.control.ControlMode;
import com.ees.ai.control.ControlProperties;
import com.ees.ai.control.ControlToolBridge;
import com.ees.ai.control.LocalControlClient;
import com.ees.ai.control.LoggingControlAuditService;
import com.ees.ai.control.RestControlClient;
import com.ees.ai.mcp.DefaultMcpClient;
import com.ees.ai.mcp.McpClient;
import com.ees.ai.mcp.McpProperties;
import com.ees.ai.mcp.McpToolBridge;
import com.ees.ai.mcp.LoggingMcpAuditService;
import com.ees.ai.mcp.McpAuditService;
import com.ees.ai.mcp.RestMcpClient;
import com.ees.metadatastore.InMemoryMetadataStore;
import com.ees.metadatastore.MetadataStore;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import com.ees.ai.api.AiChatController;

/**
 * AI/에이전트 및 MCP 연동을 위한 Spring Boot 자동 설정.
 * <p>
 * {@link ChatModel}이 클래스패스에 있을 때 활성화되며, {@code ees.ai.*}, {@code ees.mcp.*} 설정을 바인딩한다.
 */
@Configuration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties({AiAgentProperties.class, McpProperties.class, ControlProperties.class})
public class AiAutoConfiguration {

    /**
     * 기본 {@link AiToolRegistry}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    public AiToolRegistry aiToolRegistry() {
        return new DefaultAiToolRegistry();
    }

    /**
     * history-store가 metadata-store일 때 사용할 기본 {@link MetadataStore}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean(MetadataStore.class)
    @ConditionalOnProperty(prefix = "ees.ai", name = "history-store", havingValue = "metadata-store")
    public MetadataStore metadataStore() {
        return new InMemoryMetadataStore();
    }

    /**
     * history-store가 metadata-store일 때 {@link AiSessionService}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean(AiSessionService.class)
    @ConditionalOnProperty(prefix = "ees.ai", name = "history-store", havingValue = "metadata-store")
    public AiSessionService metadataStoreAiSessionService(MetadataStore metadataStore,
                                                          AiAgentProperties properties) {
        return new MetadataStoreAiSessionService(metadataStore, java.time.Duration.ofSeconds(properties.getHistoryTtlSeconds()));
    }

    /**
     * 기본 in-memory {@link AiSessionService}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean(AiSessionService.class)
    public AiSessionService aiSessionService() {
        return new InMemoryAiSessionService();
    }

    /**
     * 기본 {@link AiAgentService}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    public AiAgentService aiAgentService(ChatModel chatModel,
                                         StreamingChatModel streamingChatModel,
                                         AiSessionService aiSessionService,
                                         AiToolRegistry aiToolRegistry,
                                         AiAgentProperties aiAgentProperties,
                                         List<ToolCallback> toolCallbacks,
                                         AiRateLimiter aiRateLimiter,
                                         MeterRegistry meterRegistry) {
        return new DefaultAiAgentService(chatModel, streamingChatModel, aiSessionService, aiToolRegistry, aiAgentProperties, toolCallbacks, aiRateLimiter, meterRegistry);
    }

    /**
     * 기본 {@link AiRateLimiter}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    public AiRateLimiter aiRateLimiter(AiAgentProperties properties) {
        return new InMemoryAiRateLimiter(properties);
    }

    /**
     * 기본 Control 감사 서비스를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    public ControlAuditService controlAuditService() {
        return new LoggingControlAuditService();
    }

    /**
     * Control 호출에 사용할 {@link RestClient}를 구성한다(remote 모드).
     */
    @Bean(name = "controlRestClient")
    @ConditionalOnMissingBean(name = "controlRestClient")
    @ConditionalOnProperty(prefix = "ees.control", name = "mode", havingValue = "REMOTE")
    public RestClient controlRestClient(ControlProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeoutMillis());
        requestFactory.setReadTimeout((int) properties.getTimeoutMillis());

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory);
        if (properties.getAuthToken() != null && !properties.getAuthToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getAuthToken());
        }
        return builder.build();
    }

    /**
     * remote 모드에서 사용할 REST 기반 {@link ControlClient}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ees.control", name = "mode", havingValue = "REMOTE")
    public ControlClient restControlClient(RestClient controlRestClient) {
        return new RestControlClient(controlRestClient);
    }

    /**
     * local 모드에서 사용할 {@link ControlClient}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ees.control", name = "mode", havingValue = "LOCAL", matchIfMissing = true)
    public ControlClient localControlClient(ControlFacade facade, ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new LocalControlClient(facade, mapper);
    }

    /**
     * Control 툴을 {@link AiToolRegistry}에 브릿지하는 컴포넌트를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    public ControlToolBridge controlToolBridge(ControlClient controlClient,
                                               AiToolRegistry aiToolRegistry,
                                               ObjectProvider<ObjectMapper> objectMapperProvider,
                                               ControlAuditService auditService) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new ControlToolBridge(controlClient, aiToolRegistry, mapper, auditService);
    }

    /**
     * Control 툴 콜백 목록을 노출한다.
     */
    @Bean
    @ConditionalOnMissingBean(name = "controlToolCallbacks")
    public List<ToolCallback> controlToolCallbacks(ControlToolBridge bridge) {
        return bridge.toolCallbacks();
    }

    /**
     * MCP base-url이 없을 때 사용할 기본 {@link McpClient}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ees.mcp", name = "base-url", havingValue = "", matchIfMissing = true)
    public McpClient mcpClient() {
        return new DefaultMcpClient();
    }

    /**
     * 기본 MCP 감사 서비스를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    public McpAuditService mcpAuditService() {
        return new LoggingMcpAuditService();
    }

    /**
     * MCP 호출에 사용할 {@link RestClient}를 구성한다.
     */
    @Bean(name = "mcpWebClient")
    @ConditionalOnMissingBean(name = "mcpWebClient")
    @ConditionalOnProperty(prefix = "ees.mcp", name = "base-url")
    public RestClient mcpWebClient(McpProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getTimeoutMillis());
        requestFactory.setReadTimeout((int) properties.getTimeoutMillis());

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory);
        if (properties.getAuthToken() != null && !properties.getAuthToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getAuthToken());
        }
        return builder.build();
    }

    /**
     * MCP base-url이 있을 때 사용할 REST 기반 {@link McpClient}를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean(name = "restMcpClient")
    @ConditionalOnProperty(prefix = "ees.mcp", name = "base-url")
    @org.springframework.context.annotation.Primary
    public McpClient restMcpClient(RestClient mcpWebClient) {
        return new RestMcpClient(mcpWebClient);
    }

    /**
     * MCP 툴을 {@link AiToolRegistry}에 브릿지하는 컴포넌트를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ees.mcp", name = "enabled", havingValue = "true")
    public McpToolBridge mcpToolBridge(McpClient mcpClient, AiToolRegistry aiToolRegistry, McpAuditService auditService) {
        return new McpToolBridge(mcpClient, aiToolRegistry, auditService);
    }

    /**
     * MCP 툴 콜백 목록을 노출한다.
     */
    @Bean
    @ConditionalOnMissingBean(name = "mcpToolCallbacks")
    @ConditionalOnProperty(prefix = "ees.mcp", name = "enabled", havingValue = "true")
    public List<ToolCallback> mcpToolCallbacks(McpToolBridge bridge) {
        return bridge.toolCallbacks();
    }

    /**
     * AI Chat Web API 컨트롤러를 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ees.ai.web", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AiChatController aiChatController(com.ees.ai.core.AiAgentService aiAgentService,
                                             ObjectProvider<ControlClient> controlClient) {
        return new AiChatController(aiAgentService, controlClient);
    }

    /**
     * ChatModel이 없을 때 사용할 No-Op ChatModel을 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel noopChatModel() {
        return new NoOpChatModel();
    }

    /**
     * StreamingChatModel이 없을 때 사용할 No-Op StreamingChatModel을 등록한다.
     */
    @Bean
    @ConditionalOnMissingBean(StreamingChatModel.class)
    public StreamingChatModel noopStreamingChatModel(NoOpChatModel delegate) {
        return delegate;
    }
}
