# why-it-rose · Backend

Spring Boot 기반의 Gradle 멀티모듈 프로젝트입니다.

---

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 17 |
| Spring Boot | 3.4.3 |
| Gradle | 8.12 |
| Build DSL | Groovy (build.gradle) |
| DB | MySQL 8.x |

---

## 프로젝트 구조

```
backend/                                     ← 루트 프로젝트 (why-it-rose)
├── build.gradle                             ← 전체 공통 빌드 설정
├── settings.gradle                          ← 모듈 등록
├── gradlew / gradlew.bat
├── gradle/wrapper/
│
├── core/                                    ← 공통 예외, 응답 포맷, 상수
│   ├── build.gradle
│   └── src/main/java/com/whyitrose/core/
│
├── domain/                                  ← JPA Entity, Repository 인터페이스
│   ├── build.gradle
│   └── src/main/java/com/whyitrose/domain/
│
├── api-server/                              ← Controller, DTO, Security, Service, Repository 구현체
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/whyitrose/apiserver/
│       │   └── WhyItRoseApplication.java    ← 애플리케이션 진입점
│       └── resources/
│           ├── application.yml
│           └── application-local.yml
│
└── batch/                                   ← 스케줄러, Job, Step
    ├── build.gradle
    └── src/main/java/com/whyitrose/batch/
```

---

## 모듈 설명

### `core`
프로젝트 전체에서 공유하는 공통 요소를 담는 모듈입니다.
다른 어떤 모듈도 의존하지 않습니다.

- 공통 예외 클래스
- 공통 API 응답 포맷
- 전역 상수

### `domain`
JPA Entity와 Repository 인터페이스를 정의하는 모듈입니다.
비즈니스 로직 구현은 포함하지 않습니다.

- JPA Entity
- Spring Data JPA Repository 인터페이스
- 의존: `core`

### `api-server`
REST API 서버 모듈입니다. 실행 가능한 Spring Boot 애플리케이션입니다.

- Controller, DTO
- Service (비즈니스 로직 구현체)
- Repository 구현체
- Spring Security 설정
- 의존: `domain`, `core`

### `batch`
배치 처리 모듈입니다. 실행 가능한 Spring Boot 애플리케이션입니다.

- 스케줄러
- Spring Batch Job / Step 정의 (현재 Job 비활성: `spring.batch.job.enabled=false`)
- **stocks 적재**: LS OpenAPI `t9945`(주식마스터)로 `stocks` 테이블 UPSERT — 상세는 `docs/250-stocks-storage-spec.md` §2.6
  - DB(`DB_*`)·`LS_ACCESS_TOKEN` 설정 후 예:
    `.\gradlew.bat :batch:bootRun --args="--stock.master.load-at-startup=true"`
  - 250종목만 넣으려면 `batch/src/main/resources/seed/index-universe.json`에 `kospi200` / `kosdaq50` 티커 배열을 채움
- 의존: `domain`, `core`

---

## 모듈 의존성 방향

```
api-server ──┐
             ├──→ domain ──→ core
batch      ──┘
```

- `core`는 외부 의존성을 갖지 않는 최하위 모듈입니다.
- `domain`은 `core`만 의존합니다.
- `api-server`와 `batch`는 각각 `domain`과 `core`를 의존합니다.

---

## 빌드 설정 상세

### 루트 `build.gradle` — 공통 설정

모든 서브모듈(`subprojects`)에 아래 설정이 공통 적용됩니다.

| 항목 | 내용 |
|---|---|
| Java | sourceCompatibility / targetCompatibility = 17 |
| BOM | `spring-boot-dependencies:3.4.3` via `dependencyManagement` |
| Repository | mavenCentral() |
| Lombok | `compileOnly` + `annotationProcessor` 공통 적용 |
| 테스트 | `spring-boot-starter-test` + JUnit Platform 공통 적용 |

```groovy
// 루트 build.gradle 핵심 구조
plugins {
    id 'org.springframework.boot' version '3.4.3' apply false      // 서브모듈에서 선택적 적용
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

subprojects {
    apply plugin: 'java-library'                                   // java → java-library
    apply plugin: 'io.spring.dependency-management'

    dependencyManagement {
        imports {
            mavenBom "org.springframework.boot:spring-boot-dependencies:3.4.3"
        }
    }
    // ...
}
```

> `apply false` 이유: `org.springframework.boot` 플러그인은 실행 가능한 fat jar를 생성합니다.
> `core`와 `domain`은 다른 모듈에 의존되는 라이브러리 모듈이므로 이 플러그인을 적용하지 않습니다.
> `api-server`와 `batch` 모듈 각각의 `build.gradle`에서 명시적으로 `apply`합니다.

> `java-library` 플러그인을 사용하는 이유: `core`와 `domain`은 다른 모듈에 제공되는 라이브러리 모듈입니다.
> `java` 플러그인만 사용하면 `implementation`으로 선언한 의존성이 소비자(api-server, batch)의 컴파일 클래스패스에 노출되지 않습니다.
> `java-library`를 사용하면 `api` 키워드로 선언한 의존성을 소비자에게 전이적으로 노출할 수 있습니다.

### 각 모듈 의존성 요약

| 모듈 | 주요 의존성 |
|---|---|
| `core` | `spring-boot-starter` (**api**) |
| `domain` | `:core`, `spring-boot-starter-data-jpa` (**api**), `mysql-connector-j` (runtimeOnly) |
| `api-server` | `:domain`, `:core`, `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-dotenv` |
| `batch` | `:domain`, `:core`, `spring-boot-starter-batch`, `spring-dotenv` |

> **`api` vs `implementation` vs `runtimeOnly` 선택 기준** (`java-library` 플러그인 적용 시)
>
> | 키워드 | 의미 | 사용 예 |
> |---|---|---|
> | `api` | 소비자가 컴파일 시점에 접근해야 하는 타입을 노출 | `core`의 `spring-boot-starter`, `domain`의 `spring-boot-starter-data-jpa` |
> | `implementation` | 모듈 내부에서만 사용, 소비자에게 노출 불필요 | `core`의 내부 유틸 등 |
> | `runtimeOnly` | 런타임에만 필요, 컴파일 시 참조 없음 | `mysql-connector-j` |
>
> `domain`이 `spring-boot-starter-data-jpa`를 `implementation`으로 선언하면, `api-server`와 `batch`에서
> `JpaRepository` 등을 직접 사용할 때 컴파일 에러가 발생합니다. 따라서 `api`로 선언합니다.

---

## 설정 파일 (application.yml)

`api-server` 모듈의 `src/main/resources/`에 위치합니다.

### `application.yml`

```yaml
spring:
  application:
    name: why-it-rose
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
```

> 활성 프로파일은 `SPRING_PROFILES_ACTIVE` 환경변수로 주입합니다.
> 환경변수가 설정되지 않은 경우 기본값으로 `local`이 활성화됩니다.
> 배포 환경에서는 반드시 `SPRING_PROFILES_ACTIVE`를 명시적으로 설정해야 합니다.

### `application-local.yml`

로컬 개발 환경용 설정입니다. Git에 포함되며, 민감한 값은 환경변수로 주입합니다.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

> `ddl-auto: update`를 사용합니다. `create-drop`은 앱 종료 시 테이블이 삭제되므로 로컬 개발에 적합하지 않습니다.

> 모든 프로파일 파일(`application-dev.yml` 등)도 동일하게 환경변수로 민감 정보를 주입하고 Git에 포함합니다.

---

## 로컬 개발 환경 세팅

### 사전 준비

- Java 17 설치 확인
  ```bash
  java -version
  # openjdk version "17.x.x" ...
  ```
- MySQL 8.x 로컬 실행 및 DB 생성
  ```sql
  CREATE DATABASE whyitrose CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### 환경변수 설정

`.env.example`을 복사해 `.env`를 생성한 뒤 값을 채웁니다.
`.env`는 `.gitignore`에 등록되어 있어 Git에 커밋되지 않습니다.

```bash
cp .env.example .env
```

```dotenv
# .env
SPRING_PROFILES_ACTIVE=local

DB_URL=jdbc:mysql://localhost:3306/whyitrose?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=root
```

`spring-dotenv` 라이브러리가 애플리케이션 실행 시 루트의 `.env` 파일을 자동으로 읽어
`application-*.yml`의 `${DB_URL}` 등 placeholder에 주입합니다.

### 프로젝트 클론 및 빌드

```bash
git clone <repository-url>
cd backend

# 전체 빌드
./gradlew build

# 빌드 결과 확인 (컴파일만)
./gradlew compileJava
```

### 애플리케이션 실행

```bash
# api-server 실행 (기본 프로파일: local)
./gradlew :api-server:bootRun

# 프로파일을 명시적으로 지정하는 경우
./gradlew :api-server:bootRun --args='--spring.profiles.active=local'
```

### IntelliJ IDEA에서 열기

1. IntelliJ IDEA 실행
2. `File → Open` → `backend/` 디렉토리 선택
3. **"Open as Project"** 선택
4. Gradle 자동 임포트가 완료되면 4개 모듈이 자동으로 인식됩니다.

> IntelliJ가 Gradle을 자동으로 인식하려면 `settings.gradle`이 루트에 있어야 합니다.

---

## 자주 쓰는 Gradle 명령어

```bash
# 전체 프로젝트 빌드
./gradlew build

# 전체 테스트 실행
./gradlew test

# 특정 모듈만 빌드
./gradlew :api-server:build
./gradlew :domain:build

# 빌드 캐시 정리
./gradlew clean

# 모듈 트리 확인
./gradlew projects

# 특정 모듈의 의존성 트리 확인
./gradlew :api-server:dependencies
```

---

## .gitignore 주요 항목

| 항목 | 이유 |
|---|---|
| `.idea/`, `*.iml` | IntelliJ 개인 설정 파일 |
| `.gradle/`, `build/` | Gradle 빌드 산출물 |
| `.env` | 실제 환경변수 값 포함, 커밋 금지 |
| `.DS_Store` 외 macOS 시스템 파일 | macOS 시스템 자동 생성 파일 |
