# Stage 1: build
# Start with a Maven image that includes JDK 17
FROM maven:3.9.10-amazoncorretto-17 AS build

# Copy source code and pom.xml file to /app folder in the container
WORKDIR /app
COPY pom.xml .
COPY src ./src


# Build the application using Maven
RUN mvn clean package -DskipTests


# After stage 1, we have a JAR file in the target directory

# Stage 2: create image
# Start with Amazon Corretto 17 as the base image
FROM amazoncorretto:17.0.15

# Set working folder to /app and copy .jar file from above stage (and rename it to `app.jar`)
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Command to run the application
ENTRYPOINT [ "java", "-jar", "app.jar" ]
