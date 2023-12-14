FROM azul/zulu-openjdk-alpine:17

MAINTAINER Alexandre de Souza  <alexandre@mobin.com.br>

ENTRYPOINT ["java", "-jar", "/usr/share/api/beethoven.jar", "-Djava.net.preferIPv4Stack=true"]

RUN apk add tzdata
RUN cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime

# Add the service itself
ADD target/beethoven-*.jar /usr/share/api/beethoven.jar