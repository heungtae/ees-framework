package com.ees.ai.core;

import java.util.List;
import java.util.Optional;

/**
 * AI 도구 등록/조회 레지스트리.
 */
public interface AiToolRegistry {

    /**
     * 도구를 등록한다.
     *
     * @param tool 등록할 도구
     */
    void register(AiTool tool);

    /**
     * 이름으로 도구를 조회한다.
     *
     * @param name 도구 이름
     * @return 도구(없으면 empty)
     */
    Optional<AiTool> find(String name);

    /**
     * 등록된 도구 목록을 반환한다.
     *
     * @return 도구 목록
     */
    List<AiTool> list();
}
