#!/bin/sh
set -e

if [ -z "$TMM_API_KEY" ] && [ ! -f /data/data/tmm.json ]; then
  echo "TMM_API_KEY must be set on the first start - it enables the HTTP API and protects the web UI." >&2
  exit 1
fi

if [ ! -w /data ]; then
  echo "/data is not writable for the user $(id -u):$(id -g)." >&2
  exit 1
fi

# shellcheck disable=SC2086
exec java $JAVA_OPTS \
  -Djava.awt.headless=true \
  -Duser.home=/data \
  -Dtmm.contentfolder=/data \
  -cp "/app/tmm.jar:/app/lib/*" \
  org.tinymediamanager.TinyMediaManager --start-api
