# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM gradle:8.5-jdk17 AS builder

ARG MODULE=api-server

WORKDIR /app

# 의존성 레이어 캐싱: 각 모듈의 build.gradle을 소스보다 먼저 복사
# 소스 코드가 변경되어도 build.gradle이 동일하면 이 레이어를 재사용
COPY build.gradle settings.gradle ./
COPY core/build.gradle        core/
COPY domain/build.gradle      domain/
COPY api-server/build.gradle  api-server/
COPY batch/build.gradle       batch/
RUN gradle dependencies --no-daemon

# 전체 소스 복사 후 빌드
COPY . .
RUN ./gradlew :${MODULE}:build -x test --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

ARG MODULE=api-server

WORKDIR /app

# non-root 유저 생성 (보안)
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=builder /app/${MODULE}/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]