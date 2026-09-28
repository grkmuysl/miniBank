# ---------- Step 1: Compile ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

# ---------- Step 2: Run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]