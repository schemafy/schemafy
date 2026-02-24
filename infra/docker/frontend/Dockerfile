# Stage 1: Build
FROM node:22-alpine AS builder

WORKDIR /app

# Copy root workspace files (exclude lockfile to get correct platform-specific deps)
COPY package.json ./
COPY apps/frontend/package.json ./apps/frontend/package.json

RUN npm install --workspace=apps/frontend

COPY apps/frontend/ ./apps/frontend/

RUN npm run build --workspace=apps/frontend

# Stage 2: Serve with nginx
FROM nginx:alpine

COPY infra/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/apps/frontend/dist/ /usr/share/nginx/html/

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=5s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
