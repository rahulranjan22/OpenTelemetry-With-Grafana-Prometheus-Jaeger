# Stage 1: Build the application
FROM maven:3.8.5-openjdk-11 AS build
WORKDIR /app

# Copy the source code and resources into the container
COPY TestHttpServer.java src/main/java/
COPY log4j2.xml src/main/resources/
COPY pom.xml .
RUN apt-get update && apt-get install -y iputils-ping

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the Java application
FROM openjdk:11-jre-slim
WORKDIR /usr/src/myapp

# Copy the OpenTelemetry Java agent and the packaged JAR file from the build stage
COPY opentelemetry-javaagent.jar opentelemetry-javaagent.jar
COPY --from=build /app/target/rranjan-java-app-1.0-SNAPSHOT.jar rranjan-java-app.jar

CMD ["java", "-javaagent:opentelemetry-javaagent.jar", "-jar", "rranjan-java-app.jar"]
