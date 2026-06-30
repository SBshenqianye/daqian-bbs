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

# Auto-tag images from docker.io/library/ to localhost/ (Docker export → Podman import mismatch)
auto_tag_image() {
    local expected="$1"
    podman image exists "$expected" 2>/dev/null && return 0
    local alt="docker.io/library/${expected#localhost/}"
    if podman image exists "$alt" 2>/dev/null; then
        echo "--> Tagging $alt -> $expected"
        podman tag "$alt" "$expected"
        return $?
    fi
    return 1
}

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Create and start the BBS Podman pod from local images.

Options:
  -h, --help          Show this help message and exit

Environment variables (all optional with defaults):
  Required:
    BBS_DB_PASSWORD     PostgreSQL password (no default, must be set)

  Server ports:
    BBS_SERVER_PORT     Backend internal port          [default: 8083]
    BBS_NGINX_PORT      Nginx host mapped port         [default: 19848]
    BBS_DB_PORT         PostgreSQL host mapped port    [default: 5432]

  Paths:
    BBS_UPLOAD_DIR      Upload files host directory    [default: /data/bbs/bbsUpload]
    PG_DATA_DIR         PostgreSQL data host directory [default: /data/sql/postgre]
    SCHEMA_FILE         Path to schema SQL file        [default: scripts/bbs-pg-schema.sql]

  Pod & image:
    POD_NAME            Pod name                       [default: bbs-pod]
    BBS_SERVER_IMAGE    Backend container image tag    [default: localhost/daqian-bbs-server:offline]
    BBS_NGINX_IMAGE     Nginx container image tag      [default: localhost/daqian-bbs-nginx:offline]
    BBS_POSTGRES_IMAGE  PostgreSQL container image tag [default: localhost/daqian-bbs-postgres:12]

  Behavior:
    START_POSTGRES      Start PostgreSQL container     [default: 0]
    SKIP_SCHEMA_INIT    Skip schema initialization     [default: 0]

Examples:
  # Minimal (PostgreSQL running externally):
    BBS_DB_PASSWORD='my-pass' ./create-pod.sh

  # Full stack with PostgreSQL container:
    BBS_DB_PASSWORD='my-pass' START_POSTGRES=1 BBS_DB_PORT=15432 ./create-pod.sh

  # Custom ports and paths:
    BBS_DB_PASSWORD='my-pass' BBS_NGINX_PORT=8080 BBS_SERVER_PORT=9090 ./create-pod.sh
EOF
    exit 0
}

# Parse --help flag
for arg in "$@"; do
    case "$arg" in
        -h|--help) usage ;;
    esac
done

BBS_DB_PASSWORD="${BBS_DB_PASSWORD:?ERROR: please set BBS_DB_PASSWORD}"

# --- Ports (all configurable via env) ---
BBS_SERVER_PORT="${BBS_SERVER_PORT:-8083}"
BBS_NGINX_PORT="${BBS_NGINX_PORT:-19848}"
BBS_DB_PORT="${BBS_DB_PORT:-5432}"

# --- Paths ---
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-/data/bbs/bbsUpload}"
PG_DATA_DIR="${PG_DATA_DIR:-/data/sql/postgre}"

# --- Pod & image ---
POD_NAME="${POD_NAME:-bbs-pod}"
BBS_SERVER_IMAGE="${BBS_SERVER_IMAGE:-localhost/daqian-bbs-server:offline}"
BBS_NGINX_IMAGE="${BBS_NGINX_IMAGE:-localhost/daqian-bbs-nginx:offline}"
BBS_POSTGRES_IMAGE="${BBS_POSTGRES_IMAGE:-localhost/daqian-bbs-postgres:12}"
SCHEMA_FILE="${SCHEMA_FILE:-scripts/bbs-pg-schema.sql}"
SKIP_SCHEMA_INIT="${SKIP_SCHEMA_INIT:-0}"
START_POSTGRES="${START_POSTGRES:-0}"

# --- Database connection (for external PG or container PG) ---
BBS_DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
BBS_DB_NAME="${BBS_DB_NAME:-bbs}"
BBS_DB_USER="${BBS_DB_USER:-work_flow}"

# --- Rootless detection (WSL/dev vs RHEL/prod) ---
IS_ROOTLESS=0
if podman info --format '{{.Host.Security.Rootless}}' 2>/dev/null | grep -q "true"; then
    IS_ROOTLESS=1
fi

# --- Auto-tag images before checking ---
for img in "$BBS_SERVER_IMAGE" "$BBS_NGINX_IMAGE"; do
    auto_tag_image "$img" || require_image "$img"
done
if [ "$START_POSTGRES" = "1" ]; then
    auto_tag_image "$BBS_POSTGRES_IMAGE" || require_image "$BBS_POSTGRES_IMAGE"
fi

# --- Internal DB port for backend ---
# PG in-pod → 5432 (container default); external PG → user-configured port
if [ "$START_POSTGRES" = "1" ]; then
    BBS_DB_CONNECT_PORT=5432
else
    BBS_DB_CONNECT_PORT="$BBS_DB_PORT"
fi

echo "===== Creating BBS pod ($POD_NAME) ====="

if podman pod exists "$POD_NAME" 2>/dev/null; then
    echo "--> Pod '$POD_NAME' already exists, removing it first..."
    podman pod stop "$POD_NAME" >/dev/null 2>&1 || true
    podman pod rm -f "$POD_NAME" >/dev/null
fi

echo "--> Creating pod..."
POD_PUBLISH="--publish $BBS_NGINX_PORT:18848"
[ "$START_POSTGRES" = "1" ] && POD_PUBLISH="$POD_PUBLISH --publish $BBS_DB_PORT:5432"
podman pod create \
    --name "$POD_NAME" \
    $POD_PUBLISH \
    --label app=bbs

echo ""
if [ "$START_POSTGRES" = "1" ]; then
    echo "--> Starting PostgreSQL..."
    if [ "$IS_ROOTLESS" = "1" ]; then
        PG_VOLUME_NAME="bbs-pgdata"
        podman volume exists "$PG_VOLUME_NAME" 2>/dev/null || podman volume create "$PG_VOLUME_NAME"
        PG_VOLUME="$PG_VOLUME_NAME:/var/lib/postgresql/data"
    else
        sudo mkdir -p "$PG_DATA_DIR"
        PG_VOLUME="$PG_DATA_DIR:/var/lib/postgresql/data:Z"
    fi

    podman run --pod "$POD_NAME" --name bbs-postgres -d \
        --label app=bbs \
        -e POSTGRES_USER="$BBS_DB_USER" \
        -e POSTGRES_PASSWORD="$BBS_DB_PASSWORD" \
        -e POSTGRES_DB="$BBS_DB_NAME" \
        -v "$PG_VOLUME" \
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
if [ "$IS_ROOTLESS" = "1" ]; then
    mkdir -p "$BBS_UPLOAD_DIR" 2>/dev/null || true
else
    sudo mkdir -p "$BBS_UPLOAD_DIR"
fi

podman run --pod "$POD_NAME" --name bbs-app -d \
    --label app=bbs \
    -e BBS_DB_PASSWORD="$BBS_DB_PASSWORD" \
    -e BBS_DB_HOST="$BBS_DB_HOST" \
    -e BBS_DB_PORT="$BBS_DB_CONNECT_PORT" \
    -e BBS_DB_NAME="$BBS_DB_NAME" \
    -e BBS_DB_USER="$BBS_DB_USER" \
    -e BBS_UPLOAD_DIR=/data/bbs/bbsUpload/ \
    -e SPRING_PROFILES_ACTIVE=podman \
    -e SERVER_PORT="$BBS_SERVER_PORT" \
    -v "$BBS_UPLOAD_DIR:/data/bbs/bbsUpload:Z" \
    "$BBS_SERVER_IMAGE"

echo ""
echo "--> Starting Nginx..."
podman run --pod "$POD_NAME" --name bbs-nginx -d \
    --label app=bbs \
    -e NGINX_PORT=18848 \
    -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
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
echo "  User UI:  http://<server-ip>:${BBS_NGINX_PORT}/bbs-user/"
echo "  Admin UI: http://<server-ip>:${BBS_NGINX_PORT}/bbs-admin/"
echo "  API:      http://<server-ip>:${BBS_NGINX_PORT}/bbs-server/"
echo ""
echo "Stop:   podman pod stop $POD_NAME"
echo "Remove: podman pod rm -f $POD_NAME"
