package com.ees.framework.workflow.engine;

import reactor.core.publisher.Mono;

/**
 * 단일 Workflow 인스턴스를 표현하는 인터페이스.
 * - 이름
 * - start/stop 생명주기
 *
 * 실제 구현은 ReactorWorkflowEngine 에서 생성된다.
 */
public interface Workflow {

    /**
     * Workflow 논리 이름 (예: "order-ingest").
     */
    String getName();

    /**
     * Workflow 실행을 시작한다.
     *
     * 보통 Source(Kafka 등)를 subscribe 하고,
     * 전체 파이프라인을 연결하는 작업을 수행한다.
     */
    Mono<Void> start();

    /**
     * Workflow 실행을 중지한다.
     *
     * Source 구독 해제, 리소스 정리 등을 담당한다.
     */
    Mono<Void> stop();
}
