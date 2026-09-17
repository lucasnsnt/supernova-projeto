#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS=(
  "$ROOT_DIR/docs/DAILY_TRANSPORT_PLAN.md"
  "$ROOT_DIR/docs/STUDENT_ONBOARDING.md"
  "$ROOT_DIR/docs/TRANSPORT_MVP_VALIDATION.md"
  "$ROOT_DIR/docs/DRIVER_LED_IMPLEMENTATION.md"
)
for file in "${DOCS[@]}"; do [[ -f "$file" ]] || { echo "missing: $file" >&2; exit 1; }; done

required=(
  'convite ativo'
  'sem matrícula'
  'manifesto'
  '±30'
  'Iniciar antecipadamente'
  'Iniciar agora'
  'modal'
  'StudentSchedule'
)
for term in "${required[@]}"; do
  count=$(rg -F -i -l "$term" "${DOCS[@]}" 2>/dev/null | wc -l || true)
  if (( count == 0 )); then echo "missing concept: $term" >&2; exit 1; fi
  printf '%-28s %d docs\n' "$term" "$count"
done

printf '\nScoped implementation overview:\n'
for term in 'RecurringRoute' 'StudentSchedule' 'DailyConfirmation' 'RoutePlanning' 'route-enrollments' 'route-previews'; do
  count=$(rg -F -i -l "$term" "$ROOT_DIR/backend/src" "$ROOT_DIR/frontend/src" 2>/dev/null | wc -l || true)
  printf '%-28s %d files\n' "$term" "$count"
done

printf '\nNo assertion is made that StudentSchedule is deleted: it is retained as an academic constraint.\n'
