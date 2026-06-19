FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Run as a non-root user so a process compromise cannot escalate in the container. /app is owned
# by this user because LocalAvatarStorage writes uploaded avatars under /app/uploads at runtime.
RUN useradd --system --no-create-home --uid 10001 edulife \
    && chown -R edulife:edulife /app
USER edulife
EXPOSE 8080
ENTRYPOINT ["java","-Djava.awt.headless=true","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=65.0","-jar","/app/app.jar"]
