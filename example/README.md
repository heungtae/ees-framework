# EES Example Module (`ees-example`)

샘플 워크플로를 통해 소스→핸들러→파이프라인→싱크 흐름을 보여주는 모듈입니다. `mvn -pl example -am test` 로 테스트를 실행할 수 있습니다.

## 구성 요소
- `GreetingSource`: 고정된 인사 메시지를 방출하는 `@FxSource`.
- `GreetingSourceHandler`: 헤더에 처리자 정보를 추가하는 소스 핸들러.
- `UppercasePipelineStep`: 메시지 payload를 대문자로 변환하는 파이프라인 스텝.
- `AuditSinkHandler`: 메타데이터에 감사 정보를 추가.
- `CollectingSink`: 수신 컨텍스트를 수집하는 `@FxSink`.
- `ExampleWorkflowConfiguration`: 위 컴포넌트를 연결한 워크플로 정의.
- `ExampleWorkflowTest`: 엔드투엔드 플로우 검증 테스트.

## 빌드/테스트
```bash
mvn -pl example -am test
```

## 커스터마이징 가이드
- **소스 수정**: `GreetingSource`의 메시지 리스트를 변경하거나, 다른 타입의 payload로 확장하세요. 새로운 소스를 추가하려면 `@FxSource(type = "your-type")` 클래스와 함께 `WorkflowDsl`에서 `source("your-type")`을 가리키도록 수정합니다.
- **핸들러 추가/변경**: 헤더나 메타 처리가 필요하면 `handler` 패키지에 새로운 핸들러를 추가하고, `WorkflowDsl`의 `sourceHandlers` 또는 `sinkHandlers` 호출에 ID를 등록합니다.
- **파이프라인 스텝 확장**: 변환/의사결정 로직은 `pipeline` 패키지에 새 `@FxPipelineStep`을 추가하고 `step("your-step-id")`로 정의합니다. 순서를 바꾸거나 여러 스텝을 추가해 체인을 구성할 수 있습니다.
- **싱크 대체**: `CollectingSink` 대신 외부 시스템(예: DB, 메시지 큐)에 기록하는 싱크를 `@FxSink("your-sink-id")`로 구현한 뒤 `WorkflowDsl`의 `sink("your-sink-id")`를 업데이트합니다.
- **워크플로 정의 변경**: `ExampleWorkflowConfiguration`의 `WorkflowDsl.define` 부분에서 소스/스텝/싱크/핸들러 ID를 원하는 조합으로 재배치합니다. `ExecutionMode`를 `SEQUENTIAL`→`PARALLEL`로 바꿔 실행 방식을 조정할 수 있습니다.
- **테스트 확장**: `ExampleWorkflowTest`를 복제해 새로운 컴포넌트 조합을 검증하거나, 커스텀 소스/싱크에 대한 단위 테스트를 추가하세요.

추가 예제나 DSL 변형을 위해서는 `spring-boot-starter` 모듈의 자동 설정을 사용해 스프링 부트 앱에 통합할 수도 있습니다.
