# Image Maven officielle (JDK 21) : évite "apk add maven" qui installe openjdk25-jmods
# et échoue en CI (extract / réseau). Équivalent fiable de mvn -DskipTests package.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S yowyob && adduser -S yowyob -G yowyob
COPY --from=build /app/target/*.jar app.jar
USER yowyob
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
