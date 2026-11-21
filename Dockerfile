# ---- 1. Build the React frontend ----
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. Build the Spring Boot jar with the frontend bundled as static files ----
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests

# ---- 3. Small runtime image ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/data && chown app:app /app/data
USER app
COPY --from=backend /app/backend/target/docuchat-*.jar app.jar
ENV DOCUCHAT_STORAGE_DIR=/app/data
EXPOSE 8080
# Render and most hosts inject PORT; fall back to 8080 locally
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]
