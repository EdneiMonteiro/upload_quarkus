# Copyright (c) 2026 Ednei Monteiro. Licensed under the MIT License.
# See LICENSE and DISCLAIMER.md in the project root for details.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY web/pom.xml web/pom.xml
COPY worker/pom.xml worker/pom.xml
RUN mvn dependency:go-offline -pl shared,web -am -q || true
COPY shared/src shared/src
COPY web/src web/src
RUN mvn package -pl shared,web -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/web/target/quarkus-app /app
ENV JAVA_OPTS="-Xmx256m"
EXPOSE 8000
CMD ["java", "-jar", "/app/quarkus-run.jar"]
