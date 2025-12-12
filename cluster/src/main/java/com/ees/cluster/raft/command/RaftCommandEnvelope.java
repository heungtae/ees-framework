package com.ees.cluster.raft.command;

import java.util.Objects;

/**
 * Raft 명령을 버전/타입과 함께 감싸는 전송 단위.
 *
 * @param version 스키마/버전(1 이상)
 * @param type 명령 타입(널 불가)
 * @param command 실제 명령(널 불가)
 */
public record RaftCommandEnvelope(
        long version,
        CommandType type,
        RaftCommand command
) {

    public RaftCommandEnvelope {
        if (version <= 0) {
            throw new IllegalArgumentException("version must be > 0");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(command, "command must not be null");
        if (command.type() != type) {
            throw new IllegalArgumentException("command type " + command.type() + " does not match envelope type " + type);
        }
    }
}
