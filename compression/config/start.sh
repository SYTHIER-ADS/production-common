#/bin/sh
wget --cut-dirs=2 --no-parent -nH -r $externalconf_host/compression/app/ -P /app --reject="index.html*"
env
exec java -Djava.security.egd=file:/dev/./urandom -jar /app/s1pdgs-compression.jar --spring.config.location=/app/application.yml
