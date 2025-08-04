# Gunakan base image JDK 21
FROM eclipse-temurin:21-jdk-alpine

# Set direktori kerja di dalam container
WORKDIR /app

# Copy file JAR hasil build Maven
COPY target/api-gateway-*.jar app.jar

# Expose port default Spring Boot
EXPOSE 8080

# Jalankan aplikasi
ENTRYPOINT ["java", "-jar", "app.jar"]
