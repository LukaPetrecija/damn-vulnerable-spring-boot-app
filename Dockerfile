FROM eclipse-temurin:8-jdk AS build

RUN mkdir /workspace
WORKDIR /workspace
COPY . .

RUN sed -i 's/\r$//' gradlew && ./gradlew build

FROM eclipse-temurin:8-jre AS app

EXPOSE 8080

COPY --from=build /workspace/build/libs/*.jar damn-vulnerable-spring-boot-app.jar

ENTRYPOINT ["java", "-jar", "./damn-vulnerable-spring-boot-app.jar"]