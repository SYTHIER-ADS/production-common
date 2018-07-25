FROM registry.geohub.space/wo7/repo-maven-all:latest as build
WORKDIR /app
COPY pom.xml /app
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY src/ /app/src/
COPY dev/ /app/dev/
COPY test/ /app/test/
COPY config/ /app/config/
RUN mkdir tmp && mvn -B -s /usr/share/maven/ref/settings-docker.xml package


FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=build /app/target/s1pdgs-metadata-catalog-1.0.0.jar /app/s1pdgs-metadata-catalog.jar
COPY /config/log/log4j2.yml log4j2.yml
COPY /src/main/resources/application.yml application.yml
COPY /config/xsltDir/ /app/xsltDir/
RUN mkdir tmp
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/s1pdgs-metadata-catalog.jar", "--spring.config.location=classpath:/application.yml"]
