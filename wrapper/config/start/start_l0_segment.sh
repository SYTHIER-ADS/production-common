#!/bin/sh
/var/tmp/conf.sh
exec java -Djava.security.egd=file:/dev/./urandom -jar /app/s1pdgs-wrapper.jar --spring.config.location=/app/config/application.yml
