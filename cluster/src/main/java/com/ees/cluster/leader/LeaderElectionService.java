package com.ees.cluster.leader;

import com.ees.cluster.model.LeaderElectionMode;
import com.ees.cluster.model.LeaderInfo;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 리더 선출/해제 및 리더 변경 감시를 제공하는 서비스.
 */
public interface LeaderElectionService {

    /**
     * 리더 획득을 시도한다.
     *
     * @param groupId 그룹 ID
     * @param nodeId 후보 노드 ID
     * @param mode 선출 방식
     * @param leaseDuration 리더 리스 기간
     * @return 획득 성공 시 LeaderInfo, 실패 시 empty
     */
    Optional<LeaderInfo> tryAcquireLeader(String groupId, String nodeId, LeaderElectionMode mode, Duration leaseDuration);

    /**
     * 리더를 해제한다.
     *
     * @return 해제되면 true
     */
    boolean release(String groupId, String nodeId);

    /**
     * 현재 리더를 조회한다.
     */
    Optional<LeaderInfo> getLeader(String groupId);

    /**
     * 리더 변경을 감시한다.
     *
     * @param groupId 그룹 ID
     * @param consumer 리더 정보 소비자
     */
    void watch(String groupId, Consumer<LeaderInfo> consumer);
}
