FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S yowyob && adduser -S yowyob -G yowyob
COPY --from=build /app/target/*.jar app.jar
USER yowyob
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
