package com.ees.ai.mcp;

/**
 * Standard exception for MCP transport errors.
 */
public class McpClientException extends RuntimeException {

    private final int status;
    private final String responseBody;
    /**
     * 인스턴스를 생성한다.
     * @param status 
     * @param responseBody 
     */

    public McpClientException(int status, String responseBody) {
        super(buildMessage(status, responseBody));
        this.status = status;
        this.responseBody = responseBody;
    }
    /**
     * 인스턴스를 생성한다.
     * @param status 
     * @param responseBody 
     * @param cause 
     */

    public McpClientException(int status, String responseBody, Throwable cause) {
        super(buildMessage(status, responseBody), cause);
        this.status = status;
        this.responseBody = responseBody;
    }
    // buildMessage 동작을 수행한다.

    private static String buildMessage(int status, String body) {
        return "MCP HTTP " + status + (body == null || body.isBlank() ? "" : (": " + body));
    }
    /**
     * status를 반환한다.
     * @return 
     */

    public int getStatus() {
        return status;
    }
    /**
     * responseBody를 반환한다.
     * @return 
     */

    public String getResponseBody() {
        return responseBody;
    }
}
