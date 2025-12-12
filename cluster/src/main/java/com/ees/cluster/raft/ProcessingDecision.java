package com.ees.cluster.raft;

/**
 * 특정 요청/명령을 처리할지 여부를 표현하는 결정 모델.
 *
 * @param allowed 허용 여부
 * @param reason 결정 사유(옵션)
 */
public record ProcessingDecision(
        boolean allowed,
        String reason
) {

    /**
     * 허용 결정을 생성한다.
     *
     * @param reason 사유(옵션)
     * @return 허용 결정
     */
    public static ProcessingDecision allowed(String reason) {
        return new ProcessingDecision(true, reason);
    }

    /**
     * 거부 결정을 생성한다.
     *
     * @param reason 사유(옵션)
     * @return 거부 결정
     */
    public static ProcessingDecision denied(String reason) {
        return new ProcessingDecision(false, reason);
    }
}
