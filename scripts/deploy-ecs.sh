#!/usr/bin/env bash
# deploy-ecs.sh — register a new ECS task revision with an updated container image,
# then update the service (used by GitHub Actions deploy/promotion workflows).

set -euo pipefail

usage() {
  echo "Usage: $0 <cluster> <service> <image-uri> [container-name]" >&2
  exit 1
}

CLUSTER="${1:-}"
SERVICE="${2:-}"
IMAGE_URI="${3:-}"
CONTAINER_NAME="${4:-snakeforged-api}"

if [[ -z "$CLUSTER" || -z "$SERVICE" || -z "$IMAGE_URI" ]]; then
  usage
fi

REGION="${AWS_REGION:-us-east-1}"

SERVICE_TD="$(aws ecs describe-services \
  --cluster "$CLUSTER" \
  --services "$SERVICE" \
  --region "$REGION" \
  --query 'services[0].taskDefinition' \
  --output text)"

if [[ "$SERVICE_TD" == "None" || -z "$SERVICE_TD" ]]; then
  echo "Could not resolve task definition for service '$SERVICE' in cluster '$CLUSTER'." >&2
  exit 1
fi

TD_JSON="$(mktemp)"
NEW_TD_JSON="$(mktemp)"

aws ecs describe-task-definition \
  --task-definition "$SERVICE_TD" \
  --region "$REGION" \
  --query taskDefinition \
  --output json >"$TD_JSON"

jq --arg IMG "$IMAGE_URI" --arg CN "$CONTAINER_NAME" '
  del(
    .taskDefinitionArn,
    .revision,
    .status,
    .requiresAttributes,
    .compatibilities,
    .registeredAt,
    .registeredBy,
    .deregisteredAt
  )
  | .containerDefinitions |= map(if .name == $CN then .image = $IMG else . end)
' "$TD_JSON" >"$NEW_TD_JSON"

NEW_ARN="$(aws ecs register-task-definition \
  --cli-input-json "file://${NEW_TD_JSON}" \
  --region "$REGION" \
  --query 'taskDefinition.taskDefinitionArn' \
  --output text)"

aws ecs update-service \
  --cluster "$CLUSTER" \
  --service "$SERVICE" \
  --task-definition "$NEW_ARN" \
  --force-new-deployment \
  --region "$REGION" \
  >/dev/null

rm -f "$TD_JSON" "$NEW_TD_JSON"

echo "Updated $SERVICE with $NEW_ARN (image: $IMAGE_URI)"
