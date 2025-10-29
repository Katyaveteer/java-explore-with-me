
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY ../../pom.xml ./
COPY ../stats-dto/pom.xml ../stats-dto/
COPY ../stats-client/pom.xml ../stats-client/
COPY pom.xml ./
COPY src ./src
COPY src/main/resources/schema.sql ./schema.sql
RUN ./mvnw clean package -DskipTests
EXPOSE 9090
CMD ["java", "-jar", "target/stats-server-0.0.1-SNAPSHOT.jar"]