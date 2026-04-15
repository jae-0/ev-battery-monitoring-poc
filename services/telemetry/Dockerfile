FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
# pom.xml 먼저 복사 → 의존성 레이어 캐시 (소스 변경 시 재다운로드 방지)
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl openssl
WORKDIR /app
COPY --from=builder /app/target/telemetry-service-*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
