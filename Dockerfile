FROM openjdk:8-alpine

WORKDIR /opt

COPY target/classes/com com
RUN ls

CMD ["java","-cp",".", "com.github.maxfichtelmann.LimitedWebserver"]
