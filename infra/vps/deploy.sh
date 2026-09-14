#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 || ! "$1" =~ ^ghcr\.io/[a-z0-9._/-]+:[a-f0-9]{40}$ ]]; then
  echo "Uso: deploy.sh ghcr.io/owner/image:<commit-sha>" >&2
  exit 2
fi

readonly new_image="$1"
readonly compose_file="/opt/supernova/compose.prod.yaml"
readonly health_url="http://127.0.0.1:8081/actuator/health"

previous_image="$(docker inspect --format '{{.Config.Image}}' supernova-api 2>/dev/null || true)"

docker pull "$new_image"
SUPERNOVA_IMAGE="$new_image" docker compose -f "$compose_file" up -d --remove-orphans

healthy=false
for attempt in $(seq 1 24); do
  if curl --fail --silent "$health_url" >/dev/null; then
    healthy=true
    break
  fi
  sleep 5
done

if [[ "$healthy" == true ]]; then
  echo "Deploy concluído com saúde confirmada: $new_image"
  exit 0
fi

echo "A nova versão não ficou saudável" >&2
docker logs --tail 100 supernova-api >&2 || true

if [[ -n "$previous_image" ]]; then
  echo "Restaurando versão anterior: $previous_image" >&2
  SUPERNOVA_IMAGE="$previous_image" docker compose -f "$compose_file" up -d --remove-orphans
else
  docker compose -f "$compose_file" down
fi

exit 1
