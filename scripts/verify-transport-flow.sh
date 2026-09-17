#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-quick}"
case "$MODE" in
  check|quick|backend|frontend|all|full) ;;
  *) echo "usage: $0 [check|quick|backend|frontend|all|full]" >&2; exit 2 ;;
esac

LOG_DIR="$ROOT_DIR/.local/transport-validation"
mkdir -p "$LOG_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
LOG_FILE="$LOG_DIR/${STAMP}-${MODE}.log"
SUMMARY_FILE="$LOG_DIR/${STAMP}-${MODE}.summary"
exec 3>&1
exec > >(tee -a "$LOG_FILE" >/dev/null) 2>&1

printf 'transport validation: %s\n' "$MODE"
printf 'log: %s\n' "$LOG_FILE"

run_step() {
  local label="$1"; shift
  printf '\n== %s ==\n' "$label"
  if "$@"; then
    printf 'PASS %s\n' "$label" >> "$SUMMARY_FILE"
    printf '[PASS] %s\n' "$label" >&3
  else
    local status=$?
    printf 'FAIL %s (exit %s)\n' "$label" "$status" >> "$SUMMARY_FILE"
    printf '[FAIL] %s (exit %s); see %s\n' "$label" "$status" "$LOG_FILE" >&3
    return "$status"
  fi
}

run_in_dir() {
  local dir="$1"; shift
  (cd "$dir" && "$@")
}

: > "$SUMMARY_FILE"
if [[ "$MODE" == check ]]; then
  run_step 'shell syntax' bash -n "$ROOT_DIR/scripts/verify-transport-flow.sh"
  run_step 'scoped audit' "$ROOT_DIR/scripts/audit-transport-flow.sh"
else
  if [[ "$MODE" == quick ]]; then
    run_step 'backend compile/package (tests skipped)' run_in_dir "$ROOT_DIR/backend" ./mvnw -DskipTests package
  fi
  if [[ "$MODE" == backend || "$MODE" == all || "$MODE" == full ]]; then
    run_step 'backend focused transport suites' run_in_dir "$ROOT_DIR/backend" ./mvnw -Dtest=CompleteTransportFlowTests,DailyConfirmationServiceTests,TripPlanningServiceTests,TripServiceTests,TripTrackingServiceTests,GoogleRoutePlanningGatewayTests test
  fi
  if [[ "$MODE" == quick ]]; then
    run_step 'frontend lint' run_in_dir "$ROOT_DIR/frontend" npm run lint
    run_step 'frontend build' run_in_dir "$ROOT_DIR/frontend" npm run build
  fi
  if [[ "$MODE" == frontend || "$MODE" == all ]]; then
    run_step 'frontend targeted transport tests' run_in_dir "$ROOT_DIR/frontend" npm test -- src/features/routes/RoutesPage.test.tsx
    run_step 'frontend lint' run_in_dir "$ROOT_DIR/frontend" npm run lint
    run_step 'frontend build' run_in_dir "$ROOT_DIR/frontend" npm run build
  fi
  if [[ "$MODE" == full ]]; then
    run_step 'frontend full suite' run_in_dir "$ROOT_DIR/frontend" npm test
    run_step 'frontend lint' run_in_dir "$ROOT_DIR/frontend" npm run lint
    run_step 'frontend build' run_in_dir "$ROOT_DIR/frontend" npm run build
  fi
  if [[ "$MODE" == full ]]; then
    run_step 'backend full suite' bash -c "cd '$ROOT_DIR/backend' && ./mvnw test"
  fi
fi

printf '\nSummary (%s):\n' "$MODE" >&3
sed 's/^/[transport] /' "$SUMMARY_FILE" >&3
if rg -q '^FAIL ' "$SUMMARY_FILE"; then exit 1; fi
