FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
# pom.xml 먼저 복사 → 의존성 레이어 캐시 (소스 변경 시 재다운로드 방지)
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appuser && adduser -S appuser -G appuser
USER appuser
COPY --from=builder /app/target/telemetry-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
