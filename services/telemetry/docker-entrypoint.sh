#!/bin/sh

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
else
  echo "WARNING: Could not extract emulator certificate"
fi

# 에뮬레이터 cert SAN에 IP가 포함되어 있으므로 hostname 대신 IP로 접속
# (cosmosdb-emulator 호스트명은 cert SAN에 없음)
COSMOS_IP=$(getent hosts "${COSMOS_HOST}" | awk '{print $1}')
echo "Connecting to CosmosDB emulator at IP: ${COSMOS_IP}:${COSMOS_PORT}"

exec java \
  -Djavax.net.ssl.trustStore="${CACERTS_PATH}" \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Dazure.cosmos.uri="https://${COSMOS_IP}:${COSMOS_PORT}" \
  -jar /app/app.jar
