# Multi-stage Dockerfile for chzzk-video-highlight
#  - builder: Gradle build (JDK 21)
#  - runtime: slim JRE (non-root)

# ---------- build stage ----------
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project
# copy everything (uses Gradle daemon image user for caching)
COPY --chown=gradle:gradle . .
# assemble application JAR (skip tests for faster image builds)
RUN ./gradlew bootJar --no-daemon -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
ARG APP_HOME=/opt/app
WORKDIR ${APP_HOME}

# runtime user for security
RUN groupadd -r app && useradd -r -g app app && mkdir -p ${APP_HOME} && chown -R app:app ${APP_HOME}

# copy built artifact from builder
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# default JVM options (override with --env JAVA_OPTS)
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dspring.profiles.active=prod"
EXPOSE 8080

USER app
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /opt/app/app.jar"]

