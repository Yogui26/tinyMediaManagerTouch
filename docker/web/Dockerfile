# Headless tinyMediaManager serving only the HTTP API and the web UI (no desktop GUI, no VNC).
# The build context is prepared by the CI: tmm.jar, lib/, templates/ and external-tools.json.
FROM eclipse-temurin:21-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends libmediainfo0v5 \
    && rm -rf /var/lib/apt/lists/*

ENV LANG=C.UTF-8 \
    HOME=/data \
    JAVA_OPTS="-Xms64m -Xmx1g"

COPY tmm.jar external-tools.json /app/
COPY lib /app/lib
COPY templates /app/templates
COPY entrypoint.sh /usr/local/bin/entrypoint.sh

# the container may run as any user (e.g. 99:100 on Unraid)
RUN chmod 755 /usr/local/bin/entrypoint.sh && chmod -R a+rwX /app && mkdir -p /data && chmod 777 /data

WORKDIR /app
VOLUME /data
EXPOSE 7878

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
