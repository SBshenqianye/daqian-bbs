# ============================================
# BBS Nginx - Static file server + Reverse Proxy
# 构建时上下文应为项目根目录:
#   podman build -t bbs-nginx -f nginx/Dockerfile .
# ============================================
FROM registry.access.redhat.com/ubi7/nginx-120:latest

# 安装 envsubst（用于启动时渲染 nginx 端口配置）
USER root
RUN yum install -y gettext && yum clean all
USER 1001

# Nginx configuration template（启动时由 envsubst 渲染为最终配置）
COPY nginx/nginx.conf.template /etc/nginx/nginx.conf.template

# Frontend static files (pre-built dist, 从项目根上下文复制)
COPY bbs-ui/dist/ /usr/share/nginx/html/bbs-ui/
COPY bbs-admin-ui/dist/ /usr/share/nginx/html/bbs-admin/

EXPOSE 18848

# 启动时先渲染配置，再启动 nginx
# NGINX_PORT 和 BBS_SERVER_PORT 由 podman run -e 传入
CMD envsubst '${NGINX_PORT} ${BBS_SERVER_PORT}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && nginx -g 'daemon off;'
