FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/*.jar app.jar
USER appuser
EXPOSE 8085
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx128m", "-Xms64m", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
