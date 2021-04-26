FROM openjdk:11-slim

# install openssl
RUN DEBIAN_FRONTEND=noninteractive \
    apt-get update && \
    apt-get install -y openssl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/logs
RUN mkdir -p /opt/scripts
RUN mkdir -p /data/projects/csc/users

COPY target/pki-web.jar /opt/pki-web.jar
COPY src/main/bash/* /opt/scripts/

RUN chmod +x /opt/scripts/*

VOLUME /opt/config
VOLUME /data/projects/csc

EXPOSE 8080

CMD [ "java", "-jar", "/opt/pki-web.jar", "--spring.config.location=/opt/config/application.properties,classpath:/application.properties" ]
