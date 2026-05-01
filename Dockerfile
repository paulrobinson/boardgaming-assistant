# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

COPY pom.xml .
RUN apt-get update -qq && apt-get install -y -qq maven > /dev/null 2>&1
# Cache dependencies
RUN mvn dependency:go-offline -q

COPY src src
RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

COPY --from=build /build/target/quarkus-app /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
