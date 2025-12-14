package com.ees.framework.example.autoconfigure;

import com.ees.framework.example.pipeline.UppercasePipelineStep;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 사용자 배포 플러그인 JAR에서 권장하는 Spring Boot Bean 등록 진입점.
 * <p>
 * - 이 클래스는 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *   를 통해 로딩된다.
 * - 메인 애플리케이션의 component scan 범위와 무관하게 example 모듈 컴포넌트를 등록한다.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.ees.framework.example")
public class ExamplePluginAutoConfiguration {

    /**
     * example 모듈의 샘플 파이프라인 스텝을 등록한다.
     * <p>
     * {@link UppercasePipelineStep}은 {@code @Component}가 아니므로 플러그인에서 Bean으로 제공한다.
     *
     * @return UppercasePipelineStep
     */
    @Bean
    @ConditionalOnMissingBean
    public UppercasePipelineStep uppercasePipelineStep() {
        return new UppercasePipelineStep();
    }
}

