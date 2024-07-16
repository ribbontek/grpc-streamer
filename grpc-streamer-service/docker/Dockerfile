FROM openjdk:21-jdk-slim

ARG APP_HOME=/app
ARG JAR_FILE=/build/libs/grpc-sls-router-service-final.jar
ARG CERT_FILES=/docker/certs

WORKDIR $APP_HOME
COPY ${JAR_FILE} app.jar

RUN groupadd -r appservice && useradd -r -g appservice appservice
RUN chown -R appservice:appservice $APP_HOME

COPY ${CERT_FILES} certs/
RUN chown -R appservice:appservice certs/

USER appservice

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-DJAVA_OPTS='-Xms128m -Xmx2g'","-jar", "/app/app.jar"]
