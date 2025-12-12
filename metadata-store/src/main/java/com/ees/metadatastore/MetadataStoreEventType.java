package com.ees.metadatastore;

/**
 * {@link MetadataStoreEvent}의 이벤트 종류.
 */
public enum MetadataStoreEventType {
    /**
     * 저장/갱신 이벤트.
     */
    PUT,

    /**
     * 삭제 이벤트.
     */
    DELETE,

    /**
     * TTL 만료로 인한 제거 이벤트.
     */
    EXPIRE
}
