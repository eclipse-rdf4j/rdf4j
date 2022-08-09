# Temp to reduce image size
FROM ubuntu:jammy AS temp

RUN apt update && apt install unzip

COPY ignore/rdf4j.zip /tmp/rdf4j.zip

WORKDIR /tmp

RUN unzip -q /tmp/rdf4j.zip

# Final workbench
FROM tomcat:8.5-jre11-temurin AS wb
MAINTAINER Bart Hanssens (bart.hanssens@bosa.fgov.be)

ENV JAVA_OPTS="-Xmx2g"
ENV CATALINA_OPTS="-Dorg.eclipse.rdf4j.appdata.basedir=/var/rdf4j"

RUN adduser --system tomcat

RUN	rm -rf /usr/local/tomcat/webapps/* && \
	mkdir -p /var/rdf4j && \
	chown -R tomcat /var/rdf4j /usr/local/tomcat && \
	chmod a+x /usr/local/tomcat /usr/local/tomcat/bin /usr/local/tomcat/bin/catalina.sh

COPY --from=temp /tmp/eclipse-rdf4j*/war/*.war /usr/local/tomcat/webapps

COPY web.xml /usr/local/tomcat/conf/web.xml

USER tomcat

WORKDIR /usr/local/tomcat/

EXPOSE 8080

