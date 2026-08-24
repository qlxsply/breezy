FROM nginxinc/nginx-unprivileged:1.29.4-alpine

USER root

RUN rm -f /etc/nginx/conf.d/default.conf \
    && chown -R 101:101 /usr/share/nginx/html /etc/nginx/conf.d

COPY --chown=101:101 nginx.conf /etc/nginx/templates/default.conf.template
COPY --chown=101:101 dist/ /usr/share/nginx/html/
COPY --chown=101:101 --chmod=755 docker-runtime-config.sh /docker-entrypoint.d/10-runtime-config.sh

ENV ADMIN_API_BASE_PATH=/api \
    ADMIN_BOOTSTRAP_PATH=/admin/menu-resources

USER 101

EXPOSE 9001

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:9001/healthz || exit 1
