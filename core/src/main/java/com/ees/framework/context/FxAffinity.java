package com.ees.framework.context;

/**
 * 클러스터 라우팅 및 per-key 실행을 위한 affinity 키(kind + value).
 * <p>
 * kind/value가 모두 존재할 때만 유효한 affinity로 간주한다.
 *
 * @param kind affinity 종류(예: {@code "equipmentId"})
 * @param value affinity 값(예: {@code "EQP-1"})
 */
public record FxAffinity(String kind, String value) {

    /**
     * kind/value로 affinity를 생성한다.
     * <p>
     * 둘 다 {@code null}이면 {@link #none()}을 반환한다.
     *
     * @param kind affinity 종류
     * @param value affinity 값
     * @return 생성된 affinity
     */
    public static FxAffinity of(String kind, String value) {
        if (kind == null && value == null) {
            return none();
        }
        return new FxAffinity(kind, value);
    }

    /**
     * 빈 affinity를 반환한다.
     *
     * @return kind/value가 모두 {@code null}인 affinity
     */
    public static FxAffinity none() {
        return new FxAffinity(null, null);
    }

    /**
     * kind/value 중 하나라도 없으면 비어있다고 판단한다.
     *
     * @return 비어있으면 true
     */
    public boolean isEmpty() {
        return kind == null || value == null;
    }
}
