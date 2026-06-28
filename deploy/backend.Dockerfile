FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY backend-quarkus/ /build/backend-quarkus/

RUN mvn -f /build/backend-quarkus/pom.xml -DskipTests -Dquarkus.package.jar.type=fast-jar quarkus:build

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/backend-quarkus/target/quarkus-app/lib/ /app/lib/
COPY --from=build /build/backend-quarkus/target/quarkus-app/*.jar /app/
COPY --from=build /build/backend-quarkus/target/quarkus-app/app/ /app/app/
COPY --from=build /build/backend-quarkus/target/quarkus-app/quarkus/ /app/quarkus/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
