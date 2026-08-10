#!/usr/bin/env bash
#
# Run the backend test suite locally.
#
# Usage (from the backend/ folder):
#   ./run-tests.sh
#
# What it does:
#   1. Tears down any existing docker-compose containers (dev or test) so
#      there's no port 5432 conflict.
#   2. Starts the test Postgres container (docker-compose.test.yml).
#   3. Runs `./mvnw test`.
#   4. Tears the test container back down, whether or not the tests passed.

set -uo pipefail

# Resolve paths relative to this script's location, so it works regardless
# of the directory it's invoked from.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

COMPOSE_TEST="$REPO_ROOT/docker-compose.test.yml"
COMPOSE_DEV="$REPO_ROOT/docker-compose.yml"

cleanup() {
    echo "==> Tearing down test container..."
    docker compose -f "$COMPOSE_TEST" down -v
}
# Always run cleanup, even if tests fail or the script is interrupted.
trap cleanup EXIT

echo "==> Stopping any existing containers (dev/test) to free port 5432..."
docker compose -f "$COMPOSE_DEV" down -v 2>/dev/null || true
docker compose -f "$COMPOSE_TEST" down -v 2>/dev/null || true

echo "==> Starting test database..."
docker compose -f "$COMPOSE_TEST" up -d

echo "==> Waiting for Postgres to be ready..."
until docker compose -f "$COMPOSE_TEST" exec -T db pg_isready -U test_user -d sliceofpie_test >/dev/null 2>&1; do
    sleep 1
done

echo "==> Running tests..."
cd "$SCRIPT_DIR"
./mvnw test
TEST_EXIT_CODE=$?

exit $TEST_EXIT_CODE
