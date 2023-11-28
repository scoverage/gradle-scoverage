# Use an official OpenJDK runtime as a parent image
FROM openjdk:11-jre-slim

# Set the working directory to /app
WORKDIR /app

# Copy the built JAR file into the container at /app
COPY build/libs/your-project-name.jar /app/your-project-name.jar

# Specify the default command to run on boot
CMD ["java", "-jar", "your-project-name.jar"]
