#!/bin/sh
exec java $jvm_flags_global -Xmx512m -Djava.security.egd=file:/dev/./urandom -jar /app/s1pdgs-request-repository.jar --spring.config.location=/app/config/application.yml
