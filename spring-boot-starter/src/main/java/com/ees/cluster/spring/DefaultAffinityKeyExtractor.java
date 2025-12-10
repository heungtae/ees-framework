package com.ees.cluster.spring;

import com.ees.cluster.assignment.AffinityKeyExtractor;

import java.util.Objects;
import java.util.function.Function;

/**
 * Basic extractor that delegates to a function and uses the configured default affinity kind.
 */
public class DefaultAffinityKeyExtractor<T> implements AffinityKeyExtractor<T> {

    private final String kind;
    private final Function<T, String> extractor;

    public DefaultAffinityKeyExtractor(String kind, Function<T, String> extractor) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.extractor = Objects.requireNonNull(extractor, "extractor must not be null");
    }

    @Override
    public String kind() {
        return kind;
    }

    @Override
    public String extract(T record) {
        return extractor.apply(record);
    }
}
