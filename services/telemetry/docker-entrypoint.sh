#!/bin/sh

SPRING_PROFILE="${SPRING_PROFILES_ACTIVE:-local}"

# 로컬 환경에서만 CosmosDB Emulator 인증서 처리
if [ "$SPRING_PROFILE" = "local" ]; then
  COSMOS_HOST="${COSMOS_HOST:-cosmosdb-emulator}"
  COSMOS_PORT="${COSMOS_PORT:-8081}"
  CACERTS_PATH="${JAVA_HOME}/lib/security/cacerts"

  echo "Fetching CosmosDB emulator certificate from ${COSMOS_HOST}:${COSMOS_PORT}..."
  for i in $(seq 1 20); do
    if echo | openssl s_client -connect "${COSMOS_HOST}:${COSMOS_PORT}" -showcerts 2>/dev/null \
        | openssl x509 -outform PEM -out /tmp/cosmos.crt 2>/dev/null && [ -s /tmp/cosmos.crt ]; then
      echo "Certificate extracted (attempt ${i})"
      break
    fi
    echo "Attempt ${i}/20: emulator not ready, retrying in 3s..."
    sleep 3
  done

  if [ -s /tmp/cosmos.crt ]; then
    keytool -import -noprompt \
      -keystore "${CACERTS_PATH}" \
      -storepass changeit \
      -alias cosmosdb-emulator \
      -file /tmp/cosmos.crt 2>&1 && echo "Certificate imported into JVM truststore"
  fi

  COSMOS_IP=$(getent hosts "${COSMOS_HOST}" | awk '{print $1}')
  echo "Connecting to CosmosDB emulator at IP: ${COSMOS_IP}:${COSMOS_PORT}"

  exec java \
    -Djavax.net.ssl.trustStore="${CACERTS_PATH}" \
    -Djavax.net.ssl.trustStorePassword=changeit \
    -Dazure.cosmos.uri="https://${COSMOS_IP}:${COSMOS_PORT}" \
    -jar /app/app.jar
else
  # 클라우드 환경: 실제 Azure Cosmos DB 사용 (표준 TLS 인증서)
  echo "Cloud profile detected. Connecting to Azure Cosmos DB directly."
  exec java -jar /app/app.jar
fi
