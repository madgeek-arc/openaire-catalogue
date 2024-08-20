### Build using Maven ###
FROM maven:3.9 AS maven

COPY pom.xml /tmp/
COPY . /tmp/

WORKDIR /tmp/

RUN mvn clean package -U -DskipTests

### Create Docker Image ###
FROM openjdk:21

WORKDIR /app
COPY --from=maven /tmp/target/*.jar /app/openaire-catalogue.jar

RUN groupadd -g 10001 catalogue && \
       useradd -u 10000 -g catalogue catalogue \
       && chown -R catalogue:catalogue /app

USER catalogue
ENTRYPOINT ["java", "-jar", "/app/openaire-catalogue.jar"]