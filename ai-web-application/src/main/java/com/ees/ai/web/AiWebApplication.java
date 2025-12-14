package com.ees.ai.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone AI Web Application 진입점.
 * <p>
 * Web UI + AI Chat API는 제공하고, 제어(Control)는 원격 EES 애플리케이션의 Control API를 호출한다.
 */
@SpringBootApplication
public class AiWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiWebApplication.class, args);
    }
}

