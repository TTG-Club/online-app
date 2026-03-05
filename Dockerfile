FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /opt/app
COPY --link pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B
COPY --link src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -B -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
COPY --from=build /opt/app/target/online.ttg.club.jar online.ttg.club.jar
ENTRYPOINT ["java","-jar","online.ttg.club.jar"]
