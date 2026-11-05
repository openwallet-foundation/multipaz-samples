# Build the backend JAR first:
#   ./gradlew :terminalBackend:buildFatJar
#
# The Docker build context is this terminalBackend directory, so the prebuilt
# JAR is available at build/libs/.
FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system multipaz && useradd --system --gid multipaz multipaz

WORKDIR /app
COPY build/libs/*-all.jar /app/pos-terminal.jar

USER multipaz

EXPOSE 8110

ENTRYPOINT ["java", "-jar", "/app/pos-terminal.jar"]
