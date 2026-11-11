### Build using Maven ###
FROM maven:3.9-eclipse-temurin-25 AS maven
ARG skipTests=true

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B -U -Dmaven.test.skip=${skipTests}


### Create Docker Image ###
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app
COPY --from=maven /build/target/*.jar /app/openaire-catalogue.jar

RUN groupadd -g 10001 catalogue && \
       useradd -u 10000 -g catalogue catalogue \
       && chown -R catalogue:catalogue /app

USER catalogue
ENTRYPOINT ["java", "-jar", "/app/openaire-catalogue.jar"]
