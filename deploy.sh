#!/usr/bin/env bash
#
# Deploy the resume-analyzer-backend to a server.
#
# Usage:
#   ./deploy.sh [--skip-build] [--env-file path/to/.env.prod]
#
# Required env file must define all variables from .env.prod.example.
# Secrets are read from env vars; non-secret runtime config is passed as
# --spring.* CLI arguments so the values are visible in `ps`/logs (intentional).
#

set -euo pipefail

# ---------- Defaults ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env.prod"
SKIP_BUILD=0
PROFILE="prod"

# ---------- Parse args ----------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=1; shift ;;
    --env-file)   ENV_FILE="$2"; shift 2 ;;
    --profile)    PROFILE="$2"; shift 2 ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

# ---------- Load env file ----------
if [[ ! -f "$ENV_FILE" ]]; then
  echo "❌ Env file not found: $ENV_FILE" >&2
  echo "   Copy .env.prod.example → $ENV_FILE and fill in values." >&2
  exit 1
fi

echo "📋 Loading env from: $ENV_FILE"
set -a                # auto-export every var defined below
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

# ---------- Required vars sanity check ----------
REQUIRED=(
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  AWS_S3_BUCKET_NAME
  AWS_S3_REGION
  MAIL_USERNAME
  MAIL_PASSWORD
  APP_BASE_URL
  FRONTEND_URL
  CORS_ALLOWED_ORIGINS
)
MISSING=()
for v in "${REQUIRED[@]}"; do
  if [[ -z "${!v:-}" ]]; then MISSING+=("$v"); fi
done
if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "❌ Missing required env vars: ${MISSING[*]}" >&2
  exit 1
fi
echo "✅ All required env vars present."

# ---------- Build ----------
if [[ "$SKIP_BUILD" -eq 0 ]]; then
  echo "🔨 Building..."
  (cd "$SCRIPT_DIR" && ./mvnw -q -DskipTests=true clean package)
else
  echo "⏭️  Skipping build (--skip-build)."
fi

JAR="$(ls -t "$SCRIPT_DIR"/target/*.jar | head -n1)"
if [[ -z "$JAR" ]]; then
  echo "❌ No jar found in target/" >&2; exit 1
fi
echo "📦 Using jar: $JAR"

# ---------- Run ----------
# Strategy:
# - SECRETS (DB password, AWS keys, mail password) → passed via ENVIRONMENT (Spring picks up
#   SPRING_DATASOURCE_PASSWORD etc. automatically through relaxed binding).
# - NON-SECRET runtime overrides → passed as --spring.* CLI args so they're visible in
#   `ps` output for ops debugging.
#
# Note: anything passed via --x.y=value is logged at startup by Spring. Never put secrets here.

JAVA_OPTS="${JAVA_OPTS:--XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError}"

echo "🚀 Starting application with profile '$PROFILE'..."
exec java $JAVA_OPTS -jar "$JAR" \
  --spring.profiles.active="$PROFILE" \
  --server.port="${SERVER_PORT:-8080}" \
  --aws.s3.bucket-name="$AWS_S3_BUCKET_NAME" \
  --aws.s3.region="$AWS_S3_REGION" \
  --aws.s3.presigned-url-expiration-minutes="${AWS_PRESIGNED_URL_EXPIRATION_MINUTES:-10}" \
  --app.base-url="$APP_BASE_URL" \
  --app.frontend-url="$FRONTEND_URL" \
  --app.cors.allowed-origins="$CORS_ALLOWED_ORIGINS" \
  --app.mail.from="${MAIL_FROM:-no-reply@resume-analyzer.com}" \
  --app.mail.from-name="${MAIL_FROM_NAME:-Resume Analyzer}" \
  --app.verification.base-url="$APP_BASE_URL" \
  --app.verification.frontend-url="$FRONTEND_URL"