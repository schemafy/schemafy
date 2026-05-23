# syntax=docker/dockerfile:1.7
# Build context must be the repository root (pnpm workspace).

FROM node:20-alpine AS build
RUN corepack enable
WORKDIR /repo

# Copy workspace metadata first so pnpm can plan + cache layers.
COPY pnpm-workspace.yaml pnpm-lock.yaml package.json .npmrc ./
COPY apps/bff/package.json ./apps/bff/package.json
COPY apps/frontend/package.json ./apps/frontend/package.json
COPY apps/backend/package.json ./apps/backend/package.json

# Install dependencies (with devDeps for the build).
RUN pnpm install --filter bff... --frozen-lockfile

# Copy BFF source + build.
COPY apps/bff ./apps/bff
RUN pnpm --filter bff build

# Produce a self-contained deploy directory at /deploy. pnpm deploy
# resolves symlinks and copies only production deps + source.
RUN pnpm --filter bff --prod --legacy deploy /deploy

FROM node:20-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8080

COPY --from=build /deploy/dist ./dist
COPY --from=build /deploy/package.json ./package.json
COPY --from=build /deploy/node_modules ./node_modules

RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

EXPOSE 8080
CMD ["node","dist/main.js"]
