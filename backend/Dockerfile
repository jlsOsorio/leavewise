# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:resolve
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests package \
    && cp target/leavewise-*.jar leavewise.jar

FROM eclipse-temurin:25-jre
# curl for the compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 leavewise
WORKDIR /app
COPY --from=build /workspace/leavewise.jar leavewise.jar
USER leavewise
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/leavewise.jar"]
