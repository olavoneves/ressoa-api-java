# Imagem da API principal do Ressoa (build multi-stage).
# Build a partir da raiz do repositório:
#   docker build -f dockerfiles/Dockerfile -t ressoa-api-java .
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S ressoa && adduser -S ressoa -G ressoa
COPY --from=build /app/target/*.jar app.jar
USER ressoa
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
