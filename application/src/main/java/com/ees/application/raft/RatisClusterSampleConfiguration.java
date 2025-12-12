package com.ees.application.raft;

import com.ees.cluster.raft.RatisLeaderElectionBridge;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.RaftClientReply;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.rpc.SupportedRpcType;
import org.apache.ratis.util.NetUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Minimal Ratis client wiring that feeds leader updates into the cluster LeaderElectionService.
 * Enable with property: sample.ratis.enabled=true
 */
@Configuration
@ConditionalOnProperty(prefix = "sample.ratis", name = "enabled", havingValue = "true")
public class RatisClusterSampleConfiguration {
    /**
     * raftGroupId를 수행한다.
     * @param groupId 
     * @return 
     */

    @Bean
    public RaftGroupId raftGroupId(@Value("${sample.ratis.group-id:00000000-0000-0000-0000-000000000001}") String groupId) {
        return RaftGroupId.valueOf(UUID.fromString(groupId));
    }
    /**
     * raftPeers를 수행한다.
     * @param peers 
     * @return 
     */

    @Bean
    public List<RaftPeer> raftPeers(@Value("${sample.ratis.peers:node1:localhost:9876}") String peers) {
        return Stream.of(peers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(RatisClusterSampleConfiguration::toPeer)
                .collect(Collectors.toList());
    }
    /**
     * raftGroup를 수행한다.
     * @param groupId 
     * @param peers 
     * @return 
     */

    @Bean
    public RaftGroup raftGroup(RaftGroupId groupId, List<RaftPeer> peers) {
        return RaftGroup.valueOf(groupId, peers);
    }
    /**
     * raftClient를 수행한다.
     * @param raftGroup 
     * @return 
     */

    @Bean
    public RaftClient raftClient(RaftGroup raftGroup) {
        RaftProperties props = new RaftProperties();
        return RaftClient.newBuilder()
                .setProperties(props)
                .setRaftGroup(raftGroup)
                .build();
    }
    /**
     * ratisLeaderElectionBridge를 수행한다.
     * @param raftClient 
     * @param groupId 
     * @return 
     */

    @Bean
    public RatisLeaderElectionBridge ratisLeaderElectionBridge(RaftClient raftClient, RaftGroupId groupId) {
        return new RatisLeaderElectionBridge(raftClient, groupId);
    }
    // toPeer 동작을 수행한다.

    private static RaftPeer toPeer(String spec) {
        // Format: id:host:port
        String[] parts = spec.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid peer spec: " + spec);
        }
        String id = parts[0];
        InetSocketAddress address = NetUtils.createSocketAddr(parts[1], Integer.parseInt(parts[2]));
        return RaftPeer.newBuilder()
                .setId(RaftPeerId.valueOf(id))
                .setAddress(address)
                .build();
    }
}
