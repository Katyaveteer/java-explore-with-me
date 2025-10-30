FROM amazoncorretto:21-alpine
COPY target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java","-jar","/app.jar"]
