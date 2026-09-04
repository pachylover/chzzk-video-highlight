# Multi-stage Dockerfile for chzzk-video-highlight
#  - builder: Gradle build (JDK 21)
#  - runtime: slim JRE (non-root)

# ---------- build stage ----------
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project
# copy everything (uses Gradle daemon image user for caching)
COPY --chown=gradle:gradle . .
# ensure wrapper is executable (Windows -> Linux copy can clear exec bit)
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
ARG APP_HOME=/opt/app
WORKDIR ${APP_HOME}

# runtime user for security
# uid/gid 를 고정한다 — 호스트에 로그 디렉터리를 바인드 마운트할 때 소유자를 맞춰야 하는데,
# useradd -r 이 자동 할당하는 uid 는 베이스 이미지가 바뀌면 달라질 수 있다.
RUN groupadd -r -g 10001 app && useradd -r -u 10001 -g app app \
    && mkdir -p ${APP_HOME}/logs && chown -R app:app ${APP_HOME}

# copy built artifact from builder
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# default JVM options (override with --env JAVA_OPTS)
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=prod"
EXPOSE 8080

USER app
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /opt/app/app.jar"]

