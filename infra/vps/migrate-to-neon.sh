#!/usr/bin/env bash
set -Eeuo pipefail

readonly app_dir="/opt/supernova"
readonly env_file="$app_dir/supernova.env"
readonly compose_file="$app_dir/compose.prod.yaml"
readonly container_name="supernova-api"
readonly health_url="http://127.0.0.1:8081/actuator/health"

: "${NEON_DATABASE_URL_UNPOOLED_B64:?Missing direct Neon URL}"
: "${NEON_DATABASE_URL_B64:?Missing pooled Neon URL}"
: "${VPS_SUDO_PASSWORD_B64:?Missing sudo password}"

neon_direct="$(printf '%s' "$NEON_DATABASE_URL_UNPOOLED_B64" | base64 --decode)"
neon_pooled="$(printf '%s' "$NEON_DATABASE_URL_B64" | base64 --decode)"
sudo_password="$(printf '%s' "$VPS_SUDO_PASSWORD_B64" | base64 --decode)"
unset NEON_DATABASE_URL_UNPOOLED_B64 NEON_DATABASE_URL_B64 VPS_SUDO_PASSWORD_B64

sudo_run() {
  printf '%s\n' "$sudo_password" | sudo -S -p '' "$@"
}

for command in docker pg_dump pg_restore psql python3 curl; do
  command -v "$command" >/dev/null || {
    echo "Required command not found on VPS: $command" >&2
    exit 1
  }
done

[[ -r "$env_file" ]] || {
  echo "Cannot read $env_file" >&2
  exit 1
}
[[ -f "$compose_file" ]] || {
  echo "Cannot find $compose_file" >&2
  exit 1
}

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

if [[ "${DATABASE_URL:-}" != jdbc:postgresql://127.0.0.1:* ]]; then
  echo "The active database is not the expected local PostgreSQL; aborting." >&2
  exit 1
fi

source_database="${DATABASE_URL##*/}"
readonly source_database="${source_database%%\?*}"
readonly timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
readonly backup_dir="$app_dir/backups"
readonly dump_file="$backup_dir/pre-neon-$timestamp.dump"
readonly counts_source="$backup_dir/pre-neon-$timestamp.counts.tsv"
readonly counts_target="$backup_dir/neon-$timestamp.counts.tsv"
readonly env_backup="$backup_dir/supernova.env.pre-neon-$timestamp"
readonly staged_env="$(mktemp)"

cleanup() {
  rm -f "$staged_env"
  unset sudo_password neon_direct neon_pooled DATABASE_PASSWORD
}
trap cleanup EXIT

install -d -m 0750 "$backup_dir"
cp "$env_file" "$env_backup"
chmod 0600 "$env_backup"

source_psql() {
  PGPASSWORD="$DATABASE_PASSWORD" psql \
    --host=127.0.0.1 --port=5432 --username="$DATABASE_USERNAME" \
    --dbname="$source_database" --no-psqlrc "$@"
}

collect_source_counts() {
  source_psql --quiet --tuples-only --no-align <<'SQL'
SELECT format(
  'SELECT %L || E''\t'' || count(*) FROM %I.%I;',
  schemaname || '.' || tablename,
  schemaname,
  tablename
)
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename
\gexec
SQL
}

collect_target_counts() {
  psql "$neon_direct" --no-psqlrc --quiet --tuples-only --no-align <<'SQL'
SELECT format(
  'SELECT %L || E''\t'' || count(*) FROM %I.%I;',
  schemaname || '.' || tablename,
  schemaname,
  tablename
)
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename
\gexec
SQL
}

echo "Preflight: checking source and Neon connectivity"
source_psql --tuples-only --no-align --command='SELECT current_database(), version();' >/dev/null
psql "$neon_direct" --no-psqlrc --tuples-only --no-align \
  --command='SELECT current_database(), version();' >/dev/null

readonly current_image="$(docker inspect --format '{{.Config.Image}}' "$container_name")"
echo "Stopping API for the final consistent snapshot"
docker stop --time 30 "$container_name" >/dev/null

rollback() {
  local exit_code=$?
  if [[ $exit_code -eq 0 ]]; then
    return
  fi
  echo "Migration failed; restoring the previous database configuration" >&2
  sudo_run cp "$env_backup" "$env_file" || true
  SUPERNOVA_IMAGE="$current_image" docker compose -f "$compose_file" \
    up -d --force-recreate --remove-orphans || true
  exit "$exit_code"
}
trap rollback ERR

echo "Creating recoverable PostgreSQL backup"
PGPASSWORD="$DATABASE_PASSWORD" pg_dump \
  --host=127.0.0.1 --port=5432 --username="$DATABASE_USERNAME" \
  --dbname="$source_database" --format=custom --no-owner --no-privileges \
  --file="$dump_file"
chmod 0600 "$dump_file"

collect_source_counts | sort >"$counts_source"

echo "Restoring backup into Neon through the direct endpoint"
pg_restore --dbname="$neon_direct" --clean --if-exists --no-owner --no-privileges \
  --exit-on-error "$dump_file"

collect_target_counts | sort >"$counts_target"
if ! diff --unified=0 "$counts_source" "$counts_target"; then
  echo "Per-table row counts do not match" >&2
  false
fi

readarray -t neon_parts < <(python3 - "$neon_pooled" <<'PY'
import sys
from urllib.parse import parse_qs, urlparse

parsed = urlparse(sys.argv[1])
query = parse_qs(parsed.query)
database = parsed.path.lstrip('/')
sslmode = query.get('sslmode', ['require'])[0]
print(f"jdbc:postgresql://{parsed.hostname}:{parsed.port or 5432}/{database}?sslmode={sslmode}")
print(parsed.username or '')
print(parsed.password or '')
PY
)

[[ ${#neon_parts[@]} -eq 3 && -n "${neon_parts[0]}" && -n "${neon_parts[1]}" ]] || {
  echo "Could not parse Neon connection details" >&2
  false
}

awk '!/^DATABASE_URL=|^DATABASE_USERNAME=|^DATABASE_PASSWORD=/' "$env_backup" >"$staged_env"
printf '%s\n' \
  "DATABASE_URL=${neon_parts[0]}" \
  "DATABASE_USERNAME=${neon_parts[1]}" \
  "DATABASE_PASSWORD=${neon_parts[2]}" \
  >>"$staged_env"

sudo_run install -m 0640 -o root -g lucas "$staged_env" "$env_file"

echo "Recreating API with Neon configuration"
SUPERNOVA_IMAGE="$current_image" docker compose -f "$compose_file" \
  up -d --force-recreate --remove-orphans

healthy=false
for attempt in $(seq 1 30); do
  if curl --fail --silent --connect-timeout 2 --max-time 4 "$health_url" >/dev/null; then
    healthy=true
    break
  fi
  sleep 4
done

if [[ "$healthy" != true ]]; then
  docker logs --tail 150 "$container_name" >&2 || true
  echo "API did not become healthy with Neon" >&2
  false
fi

echo "Migration completed; source PostgreSQL and backup were kept intact."
echo "BACKUP_FILE=$dump_file"
echo "COUNTS_FILE=$counts_source"
