## Multi-stage build to ensure the JAR is available in Docker build environments (e.g., Render)
## 1) Build stage: uses Maven + JDK to compile and package the application
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml ./
RUN mvn -q -B -DskipTests dependency:go-offline

# Now copy the source code and build the JAR
COPY src ./src
RUN mvn -q -B -DskipTests package

# Runtime image
## 2) Runtime image: slim JRE with Chromium + Chromedriver for Selenium
FROM eclipse-temurin:21-jre AS runtime
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

# Copy the packaged JAR from the build stage
COPY --from=build /build/target/inscrepper-java-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
