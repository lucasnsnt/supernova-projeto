#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Execute este script com sudo" >&2
  exit 1
fi

readonly deploy_user="${SUDO_USER:-lucas}"
readonly app_dir="/opt/supernova"
readonly app_domain="api.supernova.lucasnsnt.ink"
readonly frontend_origin="https://supernova.lucasnsnt.ink"
readonly database_name="supernova"
readonly database_user="supernova"

# Evita avisos do PostgreSQL quando o diretório original do usuário não é
# acessível à conta de sistema `postgres`.
cd /tmp

for command in docker nginx psql openssl curl; do
  command -v "$command" >/dev/null || {
    echo "Comando obrigatório ausente: $command" >&2
    exit 1
  }
done

install -d -m 0750 -o "$deploy_user" -g "$deploy_user" "$app_dir"

if [[ ! -f "$app_dir/google-service-account.json" ]]; then
  install -m 0644 -o root -g "$deploy_user" /dev/null \
    "$app_dir/google-service-account.json"
fi

if [[ ! -f "$app_dir/supernova.env" ]]; then
  database_password="$(openssl rand -hex 24)"
  jwt_secret="$(openssl rand -base64 48 | tr -d '\n')"

  if ! runuser -u postgres -- psql -tAc \
      "SELECT 1 FROM pg_roles WHERE rolname = '$database_user'" | grep -q 1; then
    runuser -u postgres -- psql --set=db_password="$database_password" <<SQL
CREATE ROLE $database_user LOGIN PASSWORD :'db_password';
SQL
  else
    echo "O usuário PostgreSQL $database_user já existe; o arquivo de ambiente não será recriado" >&2
    exit 1
  fi

  if ! runuser -u postgres -- psql -tAc \
      "SELECT 1 FROM pg_database WHERE datname = '$database_name'" | grep -q 1; then
    runuser -u postgres -- createdb --owner="$database_user" "$database_name"
  fi

  install -m 0640 -o root -g "$deploy_user" /dev/null "$app_dir/supernova.env"
  printf '%s\n' \
    "DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/$database_name" \
    "DATABASE_USERNAME=$database_user" \
    "DATABASE_PASSWORD=$database_password" \
    "JWT_SECRET=$jwt_secret" \
    "ALLOWED_ORIGINS=$frontend_origin" \
    "SECURE_COOKIES=true" \
    "MAIL_DELIVERY=log" \
    "MAIL_HEALTH_ENABLED=false" \
    "GOOGLE_ROUTE_OPTIMIZATION_ENABLED=false" \
    "GOOGLE_CLOUD_PROJECT=" \
    >"$app_dir/supernova.env"
fi

cat >/etc/nginx/conf.d/supernova-rate-limit.conf <<'NGINX_RATE'
limit_req_zone $binary_remote_addr zone=supernova_api:10m rate=20r/s;
limit_req_zone $binary_remote_addr zone=supernova_auth:10m rate=5r/s;
NGINX_RATE

cat >/etc/nginx/sites-available/supernova <<NGINX_SITE
server {
    listen 80;
    listen [::]:80;
    server_name $app_domain;

    client_max_body_size 1m;

    location /api/auth/ {
        limit_req zone=supernova_auth burst=10 nodelay;
        proxy_pass http://127.0.0.1:8081;
        include proxy_params;
    }

    location / {
        limit_req zone=supernova_api burst=40 nodelay;
        proxy_pass http://127.0.0.1:8081;
        include proxy_params;
    }
}
NGINX_SITE

ln -sfn /etc/nginx/sites-available/supernova /etc/nginx/sites-enabled/supernova
nginx -t
systemctl reload nginx

echo "Bootstrap concluído. Depois que o DNS apontar para a VPS, execute:"
echo "sudo certbot --nginx -d $app_domain"
echo "O envio de e-mail permanece em modo de log até um SMTP ser configurado."
