# Build the backend JAR first:
#   ./gradlew :transitBackend:buildFatJar
#
# The Docker build context is this transitBackend directory, so the prebuilt JAR
# is available at build/libs/.
FROM eclipse-temurin:17-jre-jammy

# Keep the UID/GID stable so the host-mounted SQLite data directory can be
# owned by the non-root runtime user.
RUN groupadd --system --gid 10001 multipaz \
    && useradd --system --uid 10001 --gid multipaz multipaz

WORKDIR /app
COPY build/libs/*-all.jar /app/transit-terminal.jar

USER multipaz

EXPOSE 8011

ENTRYPOINT ["java", "-jar", "/app/transit-terminal.jar"]
