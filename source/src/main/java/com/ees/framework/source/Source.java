package com.ees.framework.source;

import reactor.core.publisher.Flux;

/**
 * 데이터 입력의 추상화.
 *
 * @param <T> 소스에서 내보내는 레코드 타입
 */
public interface Source<T> {

    /**
     * 외부로부터 데이터를 읽어 Flux로 반환한다.
     */
    Flux<T> read();
}
