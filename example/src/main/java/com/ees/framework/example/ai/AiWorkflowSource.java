package com.ees.framework.example.ai;

import com.ees.framework.annotations.FxSource;
import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxCommand;
import com.ees.framework.context.FxContext;
import com.ees.framework.context.FxHeaders;
import com.ees.framework.context.FxMessage;
import com.ees.framework.context.FxMeta;
import com.ees.framework.source.Source;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 예시 AI 워크플로용 소스.
 * 고정된 업무 지시문(system prompt)과 사용자 입력 메시지를 함께 전달한다.
 */
@FxSource(type = "ai-demo-source")
@Component
public class AiWorkflowSource implements Source<Object> {

    private static final String AFFINITY_KIND = "equipmentId";
    private static final String AFFINITY_VALUE = "ai-demo";
    private static final String AI_PROMPT_KEY = "aiPrompt";

    private final List<String> tasks;
    private final FxCommand command;
    private final String systemPrompt;
    // AtomicInteger 동작을 수행한다.
    private final AtomicInteger sequence = new AtomicInteger();
    /**
     * 인스턴스를 생성한다.
     */

    public AiWorkflowSource() {
        this(
            List.of(
                "긴급 장애 보고서를 요약하고 대응 단계를 제시해 줘",
                "주간 릴리스 노트를 핵심 변경사항 3가지로 요약해 줘"
            ),
            "너는 SRE/개발 리더를 위한 작업 요약가다. 입력된 문장을 간결히 요약하고, 반드시 한 줄 액션 아이템을 제시하라."
        );
    }
    /**
     * 인스턴스를 생성한다.
     * @param tasks 
     * @param systemPrompt 
     */

    public AiWorkflowSource(List<String> tasks, String systemPrompt) {
        this.tasks = List.copyOf(tasks);
        this.systemPrompt = systemPrompt;
        this.command = FxCommand.of("ai-demo-ingest");
    }
    /**
     * read를 수행한다.
     * @return 
     */

    @Override
    public Iterable<FxContext<Object>> read() {
        return tasks.stream()
            .map(this::buildContext)
            .toList();
    }
    // buildContext 동작을 수행한다.

    private FxContext<Object> buildContext(String task) {
        FxHeaders headers = FxHeaders.empty()
            .with("ai-task-sequence", String.valueOf(sequence.incrementAndGet()));
        FxMessage<Object> message = FxMessage.now("ai-demo-source", task);
        FxMeta meta = new FxMeta(
            "ai-demo-source",
            "ai-demo-source",
            0,
            Map.of(
                AI_PROMPT_KEY, systemPrompt
            )
        );
        FxAffinity affinity = FxAffinity.of(AFFINITY_KIND, AFFINITY_VALUE);
        return new FxContext<>(command, headers, message, meta, affinity);
    }
}
