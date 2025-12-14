package com.ees.ai.control;

/**
 * Control 호출 모드.
 * <p>
 * - {@link #LOCAL}: 동일 프로세스 내 {@link ControlFacade}를 직접 호출한다(Embedded 기본).
 * - {@link #REMOTE}: 원격 EES 애플리케이션의 Control API를 HTTP로 호출한다(Standalone 권장).
 */
public enum ControlMode {
    LOCAL,
    REMOTE
}

