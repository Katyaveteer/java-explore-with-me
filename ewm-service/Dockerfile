
FROM amazoncorretto:21-alpine-jdk
COPY ewm-service/target/-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]