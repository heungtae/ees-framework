package com.ees.cluster.membership;

import com.ees.cluster.model.ClusterNode;
import com.ees.cluster.model.ClusterNodeRecord;
import com.ees.cluster.model.MembershipEvent;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 클러스터 멤버십(노드 join/heartbeat/leave) 관리 서비스.
 */
public interface ClusterMembershipService {

    /**
     * 노드를 클러스터에 조인 처리한다.
     *
     * @param node 조인할 노드
     * @return 저장된 노드 레코드
     */
    ClusterNodeRecord join(ClusterNode node);

    /**
     * 특정 노드의 하트비트를 갱신한다.
     *
     * @param nodeId 노드 ID
     * @return 갱신된 노드 레코드
     */
    ClusterNodeRecord heartbeat(String nodeId);

    /**
     * 노드를 leave 처리한다.
     */
    void leave(String nodeId);

    /**
     * 노드 정보를 완전히 제거한다.
     */
    void remove(String nodeId);

    /**
     * 노드 레코드를 조회한다.
     */
    Optional<ClusterNodeRecord> findNode(String nodeId);

    /**
     * 현재 멤버십 뷰를 반환한다.
     */
    Map<String, ClusterNodeRecord> view();

    /**
     * 타임아웃을 감지해 노드 상태를 업데이트한다.
     */
    void detectTimeouts();

    /**
     * 멤버십 이벤트를 구독한다.
     */
    void events(Consumer<MembershipEvent> consumer);
}
