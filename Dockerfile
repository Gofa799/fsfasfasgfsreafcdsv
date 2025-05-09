
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests


CMD ["java", "-jar", "target/robux-bot-1.0-SNAPSHOT.jar"]