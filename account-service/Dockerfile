# ---------- Step 1: Compile ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY account-service/pom.xml account-service/
COPY transfer-service/pom.xml transfer-service/
COPY api-gateway/pom.xml api-gateway/
RUN chmod +x mvnw && ./mvnw -pl account-service -am dependency:go-offline -B

COPY account-service/src account-service/src
RUN ./mvnw -pl account-service -am package -DskipTests -B

# ---------- Step 2: Run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system appuser
USER appuser

COPY --from=build /app/account-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]