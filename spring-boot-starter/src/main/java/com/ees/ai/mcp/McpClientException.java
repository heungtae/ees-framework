package com.ees.ai.mcp;

/**
 * Standard exception for MCP transport errors.
 */
public class McpClientException extends RuntimeException {

    private final int status;
    private final String responseBody;

    public McpClientException(int status, String responseBody) {
        super(buildMessage(status, responseBody));
        this.status = status;
        this.responseBody = responseBody;
    }

    public McpClientException(int status, String responseBody, Throwable cause) {
        super(buildMessage(status, responseBody), cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    private static String buildMessage(int status, String body) {
        return "MCP HTTP " + status + (body == null || body.isBlank() ? "" : (": " + body));
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
