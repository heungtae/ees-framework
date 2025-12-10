package com.ees.ai.mcp;

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestMcpClientTest {

    @Test
    void startWorkflowShouldHitExpectedPath() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.baseUrl("http://localhost:8081").build();

        server.expect(requestTo("http://localhost:8081/mcp/workflows/wf-123/start"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        RestMcpClient mcp = new RestMcpClient(client);

        mcp.startWorkflow("wf-123", Map.of("k", "v"));

        server.verify();
    }

    @Test
    void errorResponseShouldSurfaceAsIllegalState() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.baseUrl("http://localhost:8081").build();

        server.expect(requestTo("http://localhost:8081/mcp/nodes"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("fail").contentType(MediaType.TEXT_PLAIN));

        RestMcpClient mcp = new RestMcpClient(client);

        Assertions.assertThatThrownBy(mcp::listNodes)
            .isInstanceOf(McpClientException.class)
            .hasMessageContaining("400")
            .hasMessageContaining("fail");

        server.verify();
    }
}
