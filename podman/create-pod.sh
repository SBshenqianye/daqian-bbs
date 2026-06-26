#!/bin/bash
# Create and start the BBS Podman pod from local images.
# Usage:
#   BBS_DB_PASSWORD='change-me' ./podman/create-pod.sh
#
# This script is offline-safe: it only uses images already loaded on the host.
set -e

require_image() {
    local image="$1"
    if ! podman image exists "$image"; then
        echo "ERROR: required image is not loaded: $image" >&2
        echo "Load offline images first: ./podman/install-offline.sh" >&2
        exit 1
    fi
}

BBS_DB_PASSWORD="${BBS_DB_PASSWORD:?ERROR: please set BBS_DB_PASSWORD}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-/home/asiayak/bbsUpload}"
PG_DATA_DIR="${PG_DATA_DIR:-/data/sql/postgre}"
POD_NAME="${POD_NAME:-bbs-pod}"
BBS_SERVER_IMAGE="${BBS_SERVER_IMAGE:-localhost/daqian-bbs-server:offline}"
BBS_NGINX_IMAGE="${BBS_NGINX_IMAGE:-localhost/daqian-bbs-nginx:offline}"
BBS_POSTGRES_IMAGE="${BBS_POSTGRES_IMAGE:-localhost/daqian-bbs-postgres:12}"
SCHEMA_FILE="${SCHEMA_FILE:-scripts/bbs-pg-schema.sql}"
SKIP_SCHEMA_INIT="${SKIP_SCHEMA_INIT:-0}"
START_POSTGRES="${START_POSTGRES:-0}"
BBS_DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
BBS_DB_PORT="${BBS_DB_PORT:-5432}"
BBS_DB_NAME="${BBS_DB_NAME:-bbs}"
BBS_DB_USER="${BBS_DB_USER:-work_flow}"

require_image "$BBS_SERVER_IMAGE"
require_image "$BBS_NGINX_IMAGE"
if [ "$START_POSTGRES" = "1" ]; then
    require_image "$BBS_POSTGRES_IMAGE"
fi

echo "===== Creating BBS pod ($POD_NAME) ====="

if podman pod exists "$POD_NAME" 2>/dev/null; then
    echo "--> Pod '$POD_NAME' already exists, removing it first..."
    podman pod stop "$POD_NAME" >/dev/null 2>&1 || true
    podman pod rm -f "$POD_NAME" >/dev/null
fi

echo "--> Creating pod..."
podman pod create \
    --name "$POD_NAME" \
    --publish 18848:18848 \
    --label app=bbs

echo ""
if [ "$START_POSTGRES" = "1" ]; then
    echo "--> Starting PostgreSQL..."
    sudo mkdir -p "$PG_DATA_DIR" 2>/dev/null || mkdir -p "$PG_DATA_DIR"

    podman run --pod "$POD_NAME" --name bbs-postgres -d \
        --label app=bbs \
        -e POSTGRES_USER="$BBS_DB_USER" \
        -e POSTGRES_PASSWORD="$BBS_DB_PASSWORD" \
        -e POSTGRES_DB="$BBS_DB_NAME" \
        -v "$PG_DATA_DIR:/var/lib/postgresql/data:Z" \
        "$BBS_POSTGRES_IMAGE"

    echo "--> Waiting for PostgreSQL to accept connections..."
    for i in $(seq 1 60); do
        if podman exec bbs-postgres psql -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -c "SELECT 1" >/dev/null 2>&1; then
            break
        fi
        if [ "$i" -eq 60 ]; then
            echo "ERROR: PostgreSQL did not become ready in time." >&2
            podman logs bbs-postgres >&2 || true
            exit 1
        fi
        sleep 2
    done

    if [ "$SKIP_SCHEMA_INIT" != "1" ] && [ -f "$SCHEMA_FILE" ]; then
        echo "--> Applying database schema: $SCHEMA_FILE"
        podman exec -i bbs-postgres psql -U "$BBS_DB_USER" -d "$BBS_DB_NAME" < "$SCHEMA_FILE"
    else
        echo "--> Skipping schema initialization."
    fi
else
    echo "--> Reusing existing PostgreSQL at $BBS_DB_HOST:$BBS_DB_PORT/$BBS_DB_NAME"
fi

echo ""
echo "--> Starting BBS application..."
sudo mkdir -p "$BBS_UPLOAD_DIR" 2>/dev/null || mkdir -p "$BBS_UPLOAD_DIR"

podman run --pod "$POD_NAME" --name bbs-app -d \
    --label app=bbs \
    -e BBS_DB_PASSWORD="$BBS_DB_PASSWORD" \
    -e BBS_DB_HOST="$BBS_DB_HOST" \
    -e BBS_DB_PORT="$BBS_DB_PORT" \
    -e BBS_DB_NAME="$BBS_DB_NAME" \
    -e BBS_DB_USER="$BBS_DB_USER" \
    -e BBS_UPLOAD_DIR=/home/asiayak/bbsUpload/ \
    -e SPRING_PROFILES_ACTIVE=podman \
    -v "$BBS_UPLOAD_DIR:/home/asiayak/bbsUpload:Z" \
    "$BBS_SERVER_IMAGE"

echo ""
echo "--> Starting Nginx..."
podman run --pod "$POD_NAME" --name bbs-nginx -d \
    --label app=bbs \
    "$BBS_NGINX_IMAGE"

echo ""
echo "===== Pod is ready ====="
echo ""
echo "Status:     podman pod ps"
echo "Containers: podman ps -a --pod"
echo "Logs:"
echo "  podman logs bbs-app"
echo "  podman logs bbs-nginx"
echo "  podman logs bbs-postgres"
echo ""
echo "URLs:"
echo "  User UI:  http://<server-ip>:18848/bbs-user/"
echo "  Admin UI: http://<server-ip>:18848/bbs-admin/"
echo "  API:      http://<server-ip>:18848/bbs-server/"
echo ""
echo "Stop:   podman pod stop $POD_NAME"
echo "Remove: podman pod rm -f $POD_NAME"
