package com.ees.ai.control;

/**
 * Control 연동(로컬/원격) 과정에서 발생하는 표준 예외.
 */
public class ControlClientException extends RuntimeException {

    private final int status;
    private final String responseBody;

    public ControlClientException(int status, String responseBody) {
        super(message(status, responseBody));
        this.status = status;
        this.responseBody = responseBody;
    }

    public ControlClientException(int status, String responseBody, Throwable cause) {
        super(message(status, responseBody), cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private static String message(int status, String body) {
        return "Control HTTP " + status + (body == null || body.isBlank() ? "" : (": " + body));
    }
}

