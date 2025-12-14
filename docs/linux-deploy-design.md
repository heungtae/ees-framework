---
title: Linux 배포 설계(Directory / Bash / Extension Plugins)
---

# 1) 목적

이 문서는 EES Framework 기반 애플리케이션을 **Linux 환경에 배포**할 때의 표준 디렉터리 구조, **bash 기반 실행 방식**, 그리고 사용자가 개발한 **Workflow/Source/Sink/PipelineStep 확장 컴포넌트**를 배포·로딩하는 방법을 정의한다.

전제:
- 런타임은 Java 21.
- 배포 대상은 “프레임워크 라이브러리” 자체가 아니라, `ees-spring-boot-starter`를 사용해 만든 **실행 애플리케이션(JAR)** 이다.
- OS 서비스 매니저(systemd)는 선택 사항으로 제공한다.

# 2) 배포 단위

배포 단위는 애플리케이션 이름(`APP_NAME`) 기준으로 구분한다.
- 예: `APP_NAME=ees-app`

배포 아티팩트는 다음을 포함한다.
- 실행 애플리케이션 JAR(`app.jar`)
- 기본 설정 템플릿(선택)
- 운영 스크립트(`bin/*.sh`)
- (선택) 확장 플러그인 JAR들

# 3) 디렉터리 레이아웃(권장)

배포 경로는 “사용자 HOME 하위”를 기준으로 한다.

권장 기본값:
- `EES_BASE=${HOME}/ees`
- `APP_HOME=${EES_BASE}/${APP_NAME}`

## 3.1 코드/실행 파일(불변, 버전 관리)
- `${APP_HOME}/`
  - `releases/${VERSION}/` (불변 릴리스 디렉터리)
    - `lib/app.jar`
    - `bin/run.sh`
    - `bin/stop.sh` (선택)
    - `bin/status.sh` (선택)
    - `plugins/` (릴리스에 포함된 플러그인, 선택)
  - `current` → `releases/${VERSION}` (심볼릭 링크)

예시:
```
${HOME}/ees/ees-app/
  releases/2025.12.13/
    bin/run.sh
    lib/app.jar
    plugins/
  current -> releases/2025.12.13
```

## 3.2 설정(가변, 운영자가 편집)
- `${APP_HOME}/config/`
  - `application.yml`
  - `app.env` (bash 환경변수 파일)
  - `logback-spring.xml` (선택)

## 3.3 데이터/상태(가변)
- `${APP_HOME}/var/`
  - `data/` (Metadata store 파일 backend 등)
  - `plugins/` (운영 중 동적으로 투입/교체되는 플러그인 JAR)

## 3.4 로그
- `${APP_HOME}/logs/`
  - `app.log` (또는 logback 설정에 따라 분할)

## 3.5 PID/런타임 파일
- `${APP_HOME}/run/`
  - `${APP_NAME}.pid`

# 4) bash 실행 방식(권장)

## 4.1 목표
- “운영자가 shell에서 쉽게 실행/중지/재시작” 가능
- 설정/플러그인을 외부 디렉터리로 분리
- 프로세스 PID 관리 및 로그 파일 경로 일관성 확보

## 4.2 실행 커맨드 구조(권장)

애플리케이션 실행은 기본적으로 다음 형태를 권장한다.

- 설정 위치:
  - `--spring.config.additional-location=file:${APP_HOME}/config/`
- 로그 위치:
  - `-Dlogging.file.path=${APP_HOME}/logs` (또는 logback으로 고정)
- 프로파일:
  - `--spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}`

## 4.3 플러그인 로딩(핵심)

사용자 확장 JAR을 런타임에 로딩하려면 “실행 JAR이 외부 classpath를 포함할 수 있어야” 한다.

권장 옵션은 다음 2가지 중 택1:

### 옵션 A(권장): Spring Boot `PropertiesLauncher` + `loader.path`
- 장점: fat jar를 유지하면서 `/plugins` 디렉터리를 추가 classpath로 주입 가능
- 실행 예:
  - `java -Dloader.path=${APP_HOME}/current/plugins,${APP_HOME}/var/plugins -cp ${APP_HOME}/current/lib/app.jar org.springframework.boot.loader.launch.PropertiesLauncher`

주의:
- 애플리케이션 JAR은 Spring Boot loader를 포함하는 실행 JAR이어야 한다.

### 옵션 B: “배포물 classpath 방식(Thin layout)”
- 장점: 표준 `-cp "lib/*:plugins/*"` 형태로 단순하게 확장 가능
- 단점: 의존성 jar를 `lib/`로 함께 배포해야 하므로 배포물 관리가 커짐
- 실행 예:
  - `java -cp "${APP_HOME}/current/lib/*:${APP_HOME}/var/plugins/*" com.yourcompany.YourApplication`

본 저장소 기준으로는 “최종 실행 애플리케이션”이 다운스트림 프로젝트에 존재하므로,
배포 대상 애플리케이션의 패키징 전략(옵션 A/B)은 그 프로젝트에서 선택한다.

# 5) 사용자 확장 배포 설계(Workflow/Source/Sink/PipelineStep)

## 5.1 확장 컴포넌트 정의
사용자가 개발하는 대상:
- `@FxSource` + `Source<?>` 구현체
- `@FxSink` + `Sink<?>` 구현체
- `@FxPipelineStep` + `PipelineStep<?, ?>` 구현체
- `WorkflowDefinition` 또는 `WorkflowGraphDefinition` Bean

## 5.2 배포 위치(권장)
확장 JAR은 다음 두 위치를 “모두” 지원하는 것을 권장한다.
- 릴리스 포함 플러그인(불변): `${APP_HOME}/current/plugins/`
- 운영 중 추가/교체 플러그인(가변): `${APP_HOME}/var/plugins/`

그리고 런처는 두 경로를 함께 로딩한다.
- `loader.path=${APP_HOME}/current/plugins,${APP_HOME}/var/plugins`

## 5.3 Spring Bean 로딩 방식(권장)
플러그인 JAR이 단순히 클래스만 포함하면 “컴포넌트 스캔 범위” 이슈로 Bean이 로딩되지 않을 수 있다.
따라서 플러그인은 아래 중 하나를 반드시 제공하는 것을 권장한다.

- (권장) Spring Boot AutoConfiguration 방식
  - 플러그인 jar에 `@AutoConfiguration` 클래스를 두고, `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
    에 해당 클래스를 등록
  - 장점: 메인 앱의 `@SpringBootApplication` 스캔 범위와 무관하게 Bean 등록 가능

- (대안) 메인 앱이 플러그인 패키지를 포함하도록 `@SpringBootApplication(scanBasePackages=...)` 확장
  - 장점: 간단
  - 단점: 플러그인 패키지 변경 시 메인 앱 수정 필요

## 5.4 버전/호환성 정책(권장)
- 플러그인은 프레임워크 API(`ees-*` 모듈) 버전에 강하게 결합한다.
- 운영 가이드:
  - `${APP_NAME}` 릴리스 버전과 플러그인 버전을 함께 관리(동일 release에 맞춰 빌드)
  - 호환성 검증 실패 시 앱 기동 실패를 허용(조용한 무시 금지)

## 5.5 배포/교체 절차(예시)
1) 플러그인 JAR 빌드: `your-plugin-1.0.0.jar`
2) 업로드: `${APP_HOME}/var/plugins/`
3) 재시작:
   - `bin/stop.sh` → `bin/run.sh`
4) 기동 로그에서 registry 등록 확인(INFO 로그)

# 6) systemd 운영(선택, user service 권장)

운영 안정성을 위해 systemd unit을 선택 제공한다.
- Unit 파일 위치(권장): `${HOME}/.config/systemd/user/${APP_NAME}.service`
- 실행은 `${APP_HOME}/current/bin/run.sh`를 호출
- 로그는 파일 또는 journald 중 택1(파일 기반을 기본 권장)

# 7) 설정 키(예시)

Kafka Source/Sink 같은 builtin 커넥터를 사용할 경우, `${APP_HOME}/config/application.yml`에 다음처럼 둔다.

```yaml
ees:
  source:
    kafka:
      enabled: true
      bootstrap-servers: localhost:9092
      group-id: ees
      topics:
        - orders
  sink:
    kafka:
      enabled: true
      bootstrap-servers: localhost:9092
      topic: out
```

# 8) 리뷰 포인트(결정 필요)

다음 항목은 실제 배포 환경/운영 요구에 따라 확정해야 한다.
- 플러그인 로딩: 옵션 A(PropertiesLauncher) vs 옵션 B(thin classpath)
- 로그 정책: 파일(logback rolling) vs journald
- 사용자 계정/권한 모델: `ees` 전용 계정 생성 여부
- 무중단 배포 필요 여부(현재 설계는 “재시작 기반”)
