#!/usr/bin/env bash
set -euo pipefail
# Run with sudo after A minio.phuocanh.me resolves to 163.61.183.153.
cd /opt/hotelapp
[[ $(id -u) = 0 ]] || { echo 'Run with sudo'; exit 1; }
getent ahostsv4 minio.phuocanh.me | awk '{print $1}' | sort -u | grep -qx '163.61.183.153' || { echo 'DNS must point to 163.61.183.153 first'; exit 1; }
install -d /var/www/letsencrypt
site=/etc/nginx/sites-available/minio.phuocanh.me
if [[ -f "$site" ]]; then cp "$site" "$site.backup-$(date -u +%Y%m%dT%H%M%SZ)"; fi
if [[ ! -f /etc/letsencrypt/live/minio.phuocanh.me/fullchain.pem ]]; then
    cat > "$site" <<'NGINX'
server {
    listen 80;
    listen [::]:80;
    server_name minio.phuocanh.me;
    location /.well-known/acme-challenge/ { root /var/www/letsencrypt; }
    location / { return 404; }
}
NGINX
    ln -sfn "$site" /etc/nginx/sites-enabled/minio.phuocanh.me
    nginx -t
    systemctl reload nginx
    certbot certonly --webroot -w /var/www/letsencrypt -d minio.phuocanh.me --non-interactive --agree-tos
fi
install -m 644 /opt/hotelapp/minio-console.conf "$site"
ln -sfn "$site" /etc/nginx/sites-enabled/minio.phuocanh.me
nginx -t
systemctl reload nginx
install -d /etc/letsencrypt/renewal-hooks/deploy
printf '#!/bin/sh\nnginx -t && systemctl reload nginx\n' > /etc/letsencrypt/renewal-hooks/deploy/hotelapp-nginx.sh
chmod 755 /etc/letsencrypt/renewal-hooks/deploy/hotelapp-nginx.sh
curl --fail --silent --show-error https://minio.phuocanh.me/ > /dev/null
printf 'MinIO Console: https://minio.phuocanh.me\n'
