#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.prod}"
IMAGE="resume-analyzer:${IMAGE_TAG:-latest}"

echo "🐳 Building Docker image..."
docker build -t "$IMAGE" .

echo "🚀 Running container..."
docker run -d --rm \
  --name resume-analyzer-prod \
  -p "${HOST_PORT:-8080}:8080" \
  --env-file "$ENV_FILE" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC" \
  "$IMAGE" \
  --aws.s3.bucket-name="$AWS_S3_BUCKET_NAME" \
  --aws.s3.region="$AWS_S3_REGION" \
  --app.base-url="$APP_BASE_URL" \
  --app.frontend-url="$FRONTEND_URL" \
  --app.cors.allowed-origins="$CORS_ALLOWED_ORIGINS"

echo "✅ Started. Logs: docker logs -f resume-analyzer-prod"