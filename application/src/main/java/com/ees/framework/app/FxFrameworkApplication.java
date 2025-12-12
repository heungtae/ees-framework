package com.ees.framework.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EES Framework를 실행 가능한 Spring Boot 애플리케이션으로 기동하는 진입점.
 */
@SpringBootApplication
public class FxFrameworkApplication {
    /**
     * main를 수행한다.
     * @param args 
     */

    public static void main(String[] args) {
        SpringApplication.run(FxFrameworkApplication.class, args);
    }
}
