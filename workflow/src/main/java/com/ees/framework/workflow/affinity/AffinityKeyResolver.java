package com.ees.framework.workflow.affinity;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxContext;

/**
 * 워크플로 실행 시 사용할 affinity(kind/value)를 FxContext 에서 추출한다.
 * 가상의 샤딩/순서 보장을 위해 각 컨텍스트에 반드시 affinity 를 부여해야 한다.
 */
public interface AffinityKeyResolver {

    /**
     * 컨텍스트에서 affinity 정보를 찾아 반환한다.
     *
     * @param context 처리 중인 컨텍스트
     * @return kind/value 를 포함한 affinity (누락 시 null 반환 가능)
     */
    FxAffinity resolve(FxContext<?> context);
}
