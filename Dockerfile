FROM maven:latest as build

WORKDIR /workspace/app

COPY pom.xml .
COPY src src

RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

VOLUME /tmp

ARG DEPENDENCY=/workspace/app/target
COPY --from=build ${DEPENDENCY}/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]