# Javadoc 작성 가이드

이 문서는 EES Framework 코드베이스에서 사용하는 Javadoc 작성 규칙을 정의합니다.

## 기본 원칙

- `public`(및 `protected`) 클래스/인터페이스/레코드/열거형에는 **클래스 레벨 Javadoc**을 작성합니다.
- `public`(및 `protected`) 메서드/생성자에는 **메서드 레벨 Javadoc**을 작성합니다.
- `private` 메서드는 **Javadoc 대신 한 줄 주석(`//`)**으로 “왜/무엇을” 하는지 설명합니다.
  - 로직이 자명하면 주석을 생략할 수 있습니다.
  - 기술적 제약/우회/규칙(비즈니스 룰)이 있는 경우 주석을 추가합니다.

## 클래스 레벨(Javadoc) 템플릿

```java
/**
 * (이 타입이 맡는 책임/역할을 1~2문장으로 요약한다.)
 * <p>
 * (필요하면 주요 협력 객체/흐름/제약을 한 문단으로 설명한다.)
 */
public class Example { }
```

## 메서드 레벨(Javadoc) 템플릿

```java
/**
 * (이 메서드가 수행하는 동작을 1문장으로 요약한다.)
 *
 * @param arg1 (파라미터 의미/제약)
 * @return (반환값 의미/특이사항)  // void면 생략
 * @throws IllegalArgumentException (언제 발생하는지) // 발생 가능 시에만
 */
public Result doWork(String arg1) { ... }
```

### `@param` / `@return` / `@throws` 작성 규칙

- `@param`은 **모든 파라미터**에 대해 작성합니다(의미가 자명하면 짧게).
- `@return`은 반환값이 있을 때 작성합니다(`void`는 생략).
- `@throws`는 “실제로 발생 가능한” 예외에 대해서만 작성합니다.

## private 메서드 주석 규칙

`private` 메서드에는 Javadoc 대신 아래 형태를 권장합니다.

```java
// (이 private 메서드가 왜 필요한지 / 어떤 규칙을 구현하는지)
private void normalize(...) { ... }
```

권장 패턴:
- “무엇을 하는지”보다 “왜 존재하는지/어떤 제약을 만족하는지”를 우선 설명
- 성능/동시성/호환성 제약이 있을 때는 반드시 명시

## 예시

### 예: 데이터 구조를 불변으로 노출

```java
/**
 * 헤더를 불변 맵으로 보관하는 값 객체.
 * <p>
 * with(...) 호출 시 복사본을 생성하여 불변성을 유지한다.
 */
public record FxHeaders(Map<String, String> values) {
    // 내부 맵을 복사/불변화하여 외부 변경을 차단한다.
    public FxHeaders { ... }
}
```

## 참고(권장)

- 가능한 한 “문장형”으로 작성하고, 구현 상세를 그대로 반복하지 않습니다.
- 리팩터링/이름 변경에도 의미가 유지되도록 “행동/계약(Contract)” 중심으로 작성합니다.
