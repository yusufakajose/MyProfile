# Backend Dockerfile (multi-stage)
# Build stage
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
# Build fat jar
RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS=""
WORKDIR /app
# Copy built jar (single jar expected)
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]


