# ──────────────────────────────────────────
# Stage 1: Build
# ──────────────────────────────────────────
FROM gradle:8.11-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ──────────────────────────────────────────
# Stage 2: Run
# ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 로그 경로 환경변수 설정
ENV LOG_PATH=/app/logs

RUN addgroup --system spring && adduser --system --ingroup spring spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app/logs

USER spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]