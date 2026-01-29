FROM maven:3-eclipse-temurin-21-alpine AS builder

# set up workdir
WORKDIR /build

# download dependencies
COPY ./pom.xml /build
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# copy sources
COPY ./src /build/src
# package
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25.0.1_8-jre

# Application Insights version
ARG APPINSIGHTS_VERSION=3.7.7

# expose server port
EXPOSE 1888

# download script for reading Docker secrets
ADD --chmod=755 https://raw.githubusercontent.com/HSLdevcom/jore4-tools/main/docker/read-secrets.sh /tmp/read-secrets.sh

# Connection string is provided as env in Kubernetes by secrets manager
# it should not be provided for other environments (local etc)
ADD --chmod=755 https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APPINSIGHTS_VERSION}/applicationinsights-agent-${APPINSIGHTS_VERSION}.jar /usr/src/jore4-tiamat/applicationinsights-agent.jar
COPY --chmod=755 ./applicationinsights.json /usr/src/jore4-tiamat/applicationinsights.json

# copy over helper scripts
COPY --chmod=755 ./script/build-jdbc-urls.sh /tmp/

# copy compiled jar from builder stage
COPY --from=builder /build/target/*.jar /usr/src/jore4-tiamat/jore4-tiamat.jar

# read Docker secrets into environment variables and run application
CMD ["/bin/bash", "-c", "source /tmp/read-secrets.sh && source /tmp/build-jdbc-urls.sh && java --add-opens java.base/java.lang=ALL-UNNAMED -javaagent:/usr/src/jore4-tiamat/applicationinsights-agent.jar -jar /usr/src/jore4-tiamat/jore4-tiamat.jar"]

HEALTHCHECK --interval=1m --timeout=5s \
    CMD curl --fail http://localhost:1888/actuator/health
