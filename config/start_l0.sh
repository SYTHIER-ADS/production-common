/bin/sh
wget --cut-dirs=1 --no-parent -nH -r $externalconf_host/wrapper/ -P /app --reject="index.html*"
/var/tmp/conf.sh && java -Djava.security.egd=file:/dev/./urandom -jar /app/s1pdgs-wrapper.jar --spring.config.location=/app/application.yml
