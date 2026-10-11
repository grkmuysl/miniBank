# ---------- Step 1: Compile ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY account-service/pom.xml account-service/
COPY transfer-service/pom.xml transfer-service/
COPY api-gateway/pom.xml api-gateway/
COPY fraud-service/pom.xml fraud-service/
RUN chmod +x mvnw && ./mvnw -pl transfer-service -am dependency:go-offline -B

COPY transfer-service/src transfer-service/src
RUN ./mvnw -pl transfer-service -am package -DskipTests -B

# ---------- Step 2: Run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system appuser
USER appuser

COPY --from=build /app/transfer-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]