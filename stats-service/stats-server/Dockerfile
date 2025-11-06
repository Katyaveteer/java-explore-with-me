FROM amazoncorretto:21-alpine-jdk
COPY target/stats-service-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
