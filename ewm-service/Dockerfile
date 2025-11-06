
FROM amazoncorretto:21-alpine-jdk
COPY target/ewm-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]