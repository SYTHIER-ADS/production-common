ARG BRANCH

FROM artifactory.coprs.esa-copernicus.eu/werum-docker/rs-core-base:${BRANCH}
ARG VERSION
ARG COMMIT_ID
ARG BRANCH_TEXT

WORKDIR /app

RUN echo "${VERSION}" >> VERSION
RUN echo "${BRANCH_TEXT}" >> VERSION
RUN echo "${COMMIT_ID}" >> VERSION

#RUN apk update && apk add p7zip
USER root
RUN apt-get update && apt-get -y install p7zip-full
USER 1001

COPY /target/rs-compression-worker-*.jar rs-compression-worker.jar
COPY /config/start.sh start.sh
COPY /config/compression.sh compression.sh
COPY /config/uncompression.sh uncompression.sh

ENTRYPOINT ["/app/start.sh"]
