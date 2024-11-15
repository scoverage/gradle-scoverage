# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jre-slim

# Set the working directory to /app
WORKDIR /app

# Copy the built JAR file into the container at /app
# COPY build/libs/gradle-scoverage.jar /app/gradle-scoverage.jar

ARG GREETING="Hello"
ARG BANDA="sahithi"
RUN echo "$GREETING, world!, $BANDA"

# Specify the default command to run on boot
CMD ["java", "-jar", "gradle-scoverage.jar"]
