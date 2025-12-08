package com.ees.ai.mcp;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RestMcpClientTest {

    @Test
    void startWorkflowShouldHitExpectedPath() {
        AtomicReference<URI> captured = new AtomicReference<>();
        ExchangeFunction exchange = req -> {
            captured.set(req.url());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                .body("ok")
                .build());
        };
        WebClient client = WebClient.builder()
            .exchangeFunction(exchange)
            .build();

        RestMcpClient mcp = new RestMcpClient(client);

        mcp.startWorkflow("wf-123", Map.of("k", "v")).block();

        Assertions.assertThat(captured.get().getPath())
            .isEqualTo("/mcp/workflows/wf-123/start");
    }

    @Test
    void errorResponseShouldSurfaceAsIllegalState() {
        ExchangeFunction exchange = req -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST)
            .body("fail")
            .build());
        WebClient client = WebClient.builder()
            .exchangeFunction(exchange)
            .build();
        RestMcpClient mcp = new RestMcpClient(client);

        Assertions.assertThatThrownBy(() -> mcp.listNodes().block())
            .isInstanceOf(McpClientException.class)
            .hasMessageContaining("400")
            .hasMessageContaining("fail");
    }

    @Test
    void shouldRetryOnServerError() {
        Queue<ClientResponse> responses = new ArrayDeque<>();
        responses.add(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body("oops").build());
        responses.add(ClientResponse.create(HttpStatus.BAD_GATEWAY).body("oops2").build());
        responses.add(ClientResponse.create(HttpStatus.OK).body("ok").build());
        AtomicInteger attempts = new AtomicInteger();

        ExchangeFunction exchange = req -> {
            attempts.incrementAndGet();
            return Mono.just(responses.poll());
        };

        RestMcpClient client = new RestMcpClient(WebClient.builder().exchangeFunction(exchange).build());

        StepVerifier.create(client.listNodes())
            .expectNext("ok")
            .verifyComplete();

        Assertions.assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void shouldNotRetryOnClientError() {
        AtomicInteger attempts = new AtomicInteger();
        ExchangeFunction exchange = req -> {
            attempts.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).body("bad").build());
        };
        RestMcpClient client = new RestMcpClient(WebClient.builder().exchangeFunction(exchange).build());

        StepVerifier.create(client.listNodes())
            .expectError(McpClientException.class)
            .verify();

        Assertions.assertThat(attempts.get()).isEqualTo(1);
    }
}
