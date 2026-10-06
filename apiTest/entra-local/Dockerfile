# Entra Local with two changes needed to validate tokens the way a real tenant's are validated:
#
#  1. oid. Upstream deliberately omits it from app-only tokens ("no oid/scp"), but real Entra sets
#     it to the service principal object id, which for an app-only token equals sub. Without it
#     EntraTokenValidator.assertAppOnly rejects every token as DELEGATED_TOKEN.
#  2. A TLS certificate naming the hostname the app reaches it on, plus a matching truststore.
#     Upstream generates a self-signed certificate for localhost only, and the Nimbus JWKS fetch
#     uses the JVM truststore, so neither the hostname nor the issuer would verify.
#
# Local demonstration only — the key material below is generated at build time and public.

FROM alpine/openssl:3.5.4 AS certgen
RUN openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
      -keyout /tmp/tls.key -out /tmp/tls.crt \
      -subj "/CN=entra-local" \
      -addext "subjectAltName=DNS:entra-local,DNS:localhost,IP:127.0.0.1"

FROM eclipse-temurin:25-jdk-alpine AS truststore
COPY --from=certgen /tmp/tls.crt /tmp/tls.crt
RUN mkdir -p /certs \
 && cp "$JAVA_HOME/lib/security/cacerts" /certs/truststore.jks \
 && keytool -importcert -noprompt -alias entra-local \
      -file /tmp/tls.crt -keystore /certs/truststore.jks -storepass changeit

FROM ghcr.io/cmaneu/entra-local@sha256:b29df9b2f8b406796316555e24dc31cd1d1d493b10c06e10c46389be7d1a9035
USER root
RUN sed -i 's/roles: \[\.\.\.roles\],/oid: app.appId,\n        roles: [...roles],/' /app/dist/tokens/claims.js \
 && grep -q "oid: app.appId," /app/dist/tokens/claims.js
COPY --from=certgen --chmod=444 /tmp/tls.crt /certs/tls.crt
COPY --from=certgen --chmod=444 /tmp/tls.key /certs/tls.key
COPY --from=truststore --chmod=444 /certs/truststore.jks /certs/truststore.jks
ENV TLS_CERT=/certs/tls.crt \
    TLS_KEY=/certs/tls.key
