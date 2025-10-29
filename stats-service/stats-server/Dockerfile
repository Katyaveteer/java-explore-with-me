FROM amazoncorretto:21-alpine
WORKDIR /app
COPY target/stats-server-0.0.1-SNAPSHOT.jar ./stats-server.jar
EXPOSE 9090
CMD ["java", "-jar", "stats-server.jar"]
