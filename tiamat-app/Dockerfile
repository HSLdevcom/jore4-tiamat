FROM maven:3-eclipse-temurin-25 AS builder

# set up workdir
WORKDIR /build

# download dependencies
COPY ./pom.xml ./pom.xml
COPY ./tiamat-core/pom.xml ./tiamat-core/pom.xml
COPY ./tiamat-app/pom.xml ./tiamat-app/pom.xml
RUN mvn --batch-mode --no-transfer-progress -pl tiamat-core -am dependency:go-offline

# copy sources
COPY ./tiamat-core/src ./tiamat-core/src
COPY ./tiamat-app/src ./tiamat-app/src
# package
RUN mvn --batch-mode --no-transfer-progress -pl tiamat-app -am clean package -DskipTests

FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# expose server port
EXPOSE 1888

# download script for reading Docker secrets
ADD --chmod=755 https://raw.githubusercontent.com/HSLdevcom/jore4-tools/main/docker/read-secrets.sh /tmp/read-secrets.sh

# copy over helper scripts
COPY --chmod=755 ./script/build-jdbc-urls.sh /tmp/

# copy compiled jar from builder stage
COPY --from=builder /build/tiamat-app/target/jore4-tiamat.jar /usr/src/jore4-tiamat/jore4-tiamat.jar

# read Docker secrets into environment variables and run application
CMD ["/bin/bash", "-c", "source /tmp/read-secrets.sh && source /tmp/build-jdbc-urls.sh && exec java --add-opens java.base/java.lang=ALL-UNNAMED -jar /usr/src/jore4-tiamat/jore4-tiamat.jar"]

HEALTHCHECK --interval=1m --timeout=5s \
    CMD curl --fail http://localhost:1888/actuator/health
