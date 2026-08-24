#!/bin/sh
set -eu

HTML_ROOT=/usr/share/nginx/html
API_BASE_PATH=${ADMIN_API_BASE_PATH:-/api}
BOOTSTRAP_PATH=${ADMIN_BOOTSTRAP_PATH:-/admin/menu-resources}

fail() {
  printf '%s\n' "runtime config error: $1" >&2
  exit 1
}

validate_path() {
  value=$1
  name=$2
  printf '%s' "$value" | grep -Eq '^/[A-Za-z0-9._~/-]*$' || fail "$name must be a same-origin path"
  case "$value" in
    //*) fail "$name must not be protocol-relative" ;;
    /) fail "$name must not be /" ;;
    */) fail "$name must not end with /" ;;
  esac
}

validate_api_base_path() {
  value=$1
  printf '%s' "$value" | grep -Eq '^/[A-Za-z][A-Za-z0-9_-]*$' \
    || fail "ADMIN_API_BASE_PATH must be a single safe path segment"
  case "$value" in
    /admin|/healthz) fail "ADMIN_API_BASE_PATH conflicts with a reserved frontend path" ;;
  esac
}

[ -n "${API_UPSTREAM:-}" ] || fail "API_UPSTREAM is required"
printf '%s' "$API_UPSTREAM" | grep -Eq '^https?://[-A-Za-z0-9._:]+$' \
  || fail "API_UPSTREAM must be an HTTP/HTTPS origin without a path"

validate_api_base_path "$API_BASE_PATH"
validate_path "$BOOTSTRAP_PATH" ADMIN_BOOTSTRAP_PATH

temp_file="$HTML_ROOT/runtime-config.json.tmp"
printf '{\n  "apiBaseUrl": "%s",\n  "bootstrapPath": "%s"\n}\n' \
  "$API_BASE_PATH" "$BOOTSTRAP_PATH" > "$temp_file"
mv "$temp_file" "$HTML_ROOT/runtime-config.json"
