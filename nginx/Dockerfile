# ============================================
# BBS Nginx - Static file server + Reverse Proxy
# 构建时上下文应为项目根目录:
#   podman build -t bbs-nginx -f nginx/Dockerfile .
# ============================================
FROM registry.access.redhat.com/ubi7/nginx-120:latest

# Nginx configuration
COPY nginx/nginx.conf /etc/nginx/nginx.conf

# Frontend static files (pre-built dist, 从项目根上下文复制)
COPY bbs-ui/dist/ /usr/share/nginx/html/bbs-ui/
COPY bbs-admin-ui/dist/ /usr/share/nginx/html/bbs-admin/

EXPOSE 18848

CMD ["nginx", "-g", "daemon off;"]
