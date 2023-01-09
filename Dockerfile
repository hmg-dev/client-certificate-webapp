FROM alpine:3.17

ARG version=11.0.17.8.1

# Please note that the THIRD-PARTY-LICENSE could be out of date if the base image has been updated recently. 
# The Corretto team will update this file but you may see a few days' delay.
RUN wget -O /THIRD-PARTY-LICENSES-20200824.tar.gz https://corretto.aws/downloads/resources/licenses/alpine/THIRD-PARTY-LICENSES-20200824.tar.gz && \
    echo "82f3e50e71b2aee21321b2b33de372feed5befad6ef2196ddec92311bc09becb  /THIRD-PARTY-LICENSES-20200824.tar.gz" | sha256sum -c - && \
    tar x -ovzf THIRD-PARTY-LICENSES-20200824.tar.gz && \
    rm -rf THIRD-PARTY-LICENSES-20200824.tar.gz && \
    wget -O /etc/apk/keys/amazoncorretto.rsa.pub https://apk.corretto.aws/amazoncorretto.rsa.pub && \
    SHA_SUM="6cfdf08be09f32ca298e2d5bd4a359ee2b275765c09b56d514624bf831eafb91" && \
    echo "${SHA_SUM}  /etc/apk/keys/amazoncorretto.rsa.pub" | sha256sum -c - && \
    echo "https://apk.corretto.aws" >> /etc/apk/repositories && \
    apk add --no-cache amazon-corretto-11=$version-r0
    
ENV LANG C.UTF-8

ENV JAVA_HOME=/usr/lib/jvm/default-jvm
ENV PATH=$PATH:/usr/lib/jvm/default-jvm/bin

#
# WARNING:
# amazoncorretto:11-alpine3.17 isn't out yet: https://hub.docker.com/_/amazoncorretto/tags?page=1&name=3.17
# But already prepared: https://github.com/corretto/corretto-docker/blob/main/11/jdk/alpine/3.17/Dockerfile
#
# Delete the above lines (and uncomment the following FROM-line) as soon as it is!!!
# 
# FROM amazoncorretto:11-alpine

# install openssl
RUN apk update && apk add openssh openssl git bash

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
