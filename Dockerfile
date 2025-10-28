# Build stage (optional if using Maven locally)
FROM eclipse-temurin:21-jdk as build
WORKDIR /app
COPY . .
# RUN ./mvnw -q -DskipTests package

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Chromium & chromedriver for Selenium (Debian-based)
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       chromium \
       chromium-driver \
       fonts-liberation \
       ca-certificates \
       wget \
    && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium
ENV CHROME_DRIVER=/usr/bin/chromedriver

# Copy jar built externally (adjust path if using multi-stage build)
COPY target/inscrepper-java-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
