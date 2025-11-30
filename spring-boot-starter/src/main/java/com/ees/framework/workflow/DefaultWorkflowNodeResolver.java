package com.ees.framework.workflow;

import com.ees.framework.handlers.SinkHandler;
import com.ees.framework.handlers.SourceHandler;
import com.ees.framework.pipeline.PipelineStep;
import com.ees.framework.registry.PipelineStepRegistry;
import com.ees.framework.registry.SinkHandlerRegistry;
import com.ees.framework.registry.SinkRegistry;
import com.ees.framework.registry.SourceHandlerRegistry;
import com.ees.framework.registry.SourceRegistry;
import com.ees.framework.sink.Sink;
import com.ees.framework.source.Source;
import com.ees.framework.workflow.engine.WorkflowNodeResolver;
import com.ees.framework.workflow.model.WorkflowNodeDefinition;
import com.ees.framework.workflow.model.WorkflowNodeKind;

/**
 * WorkflowNodeDefinition 의 kind/refName 을 이용해서
 * 실제 실행 객체(Source, Handler, PipelineStep, Sink)를 resolve 하는 기본 구현.
 */
public class DefaultWorkflowNodeResolver implements WorkflowNodeResolver {

    private final SourceRegistry sourceRegistry;
    private final SourceHandlerRegistry sourceHandlerRegistry;
    private final PipelineStepRegistry pipelineStepRegistry;
    private final SinkHandlerRegistry sinkHandlerRegistry;
    private final SinkRegistry sinkRegistry;

    public DefaultWorkflowNodeResolver(
        SourceRegistry sourceRegistry,
        SourceHandlerRegistry sourceHandlerRegistry,
        PipelineStepRegistry pipelineStepRegistry,
        SinkHandlerRegistry sinkHandlerRegistry,
        SinkRegistry sinkRegistry
    ) {
        this.sourceRegistry = sourceRegistry;
        this.sourceHandlerRegistry = sourceHandlerRegistry;
        this.pipelineStepRegistry = pipelineStepRegistry;
        this.sinkHandlerRegistry = sinkHandlerRegistry;
        this.sinkRegistry = sinkRegistry;
    }

    @Override
    public Object resolve(WorkflowNodeDefinition node) {
        WorkflowNodeKind kind = node.getKind();
        String ref = node.getRefName();

        return switch (kind) {
            case SOURCE -> {
                Source<?> src = sourceRegistry.getByType(ref);
                yield src;
            }
            case SOURCE_HANDLER -> {
                SourceHandler<?> h = sourceHandlerRegistry.getByName(ref);
                yield h;
            }
            case PIPELINE_STEP -> {
                PipelineStep<?, ?> step = pipelineStepRegistry.getByName(ref);
                yield step;
            }
            case SINK_HANDLER -> {
                SinkHandler<?> h = sinkHandlerRegistry.getByName(ref);
                yield h;
            }
            case SINK -> {
                Sink<?> sink = sinkRegistry.getByType(ref);
                yield sink;
            }
        };
    }
}
