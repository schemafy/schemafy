# syntax=docker/dockerfile:1.7
# Build context must be the repository root (pnpm workspace).

FROM node:20-alpine AS deps
RUN corepack enable
WORKDIR /repo

COPY pnpm-workspace.yaml pnpm-lock.yaml package.json .npmrc ./
COPY apps/bff/package.json ./apps/bff/package.json
COPY apps/frontend/package.json ./apps/frontend/package.json
COPY apps/backend/package.json ./apps/backend/package.json

RUN pnpm install --filter bff... --frozen-lockfile

FROM node:20-alpine AS build
RUN corepack enable
WORKDIR /repo

COPY --from=deps /repo/node_modules ./node_modules
COPY --from=deps /repo/apps/bff/node_modules ./apps/bff/node_modules
COPY pnpm-workspace.yaml pnpm-lock.yaml package.json .npmrc ./
COPY apps/bff ./apps/bff
COPY apps/frontend/package.json ./apps/frontend/package.json
COPY apps/backend/package.json ./apps/backend/package.json

RUN pnpm --filter bff build

FROM node:20-alpine AS runtime
RUN corepack enable
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8080

COPY --from=build /repo/apps/bff/dist ./dist
COPY --from=build /repo/apps/bff/package.json ./package.json
COPY --from=build /repo/apps/bff/node_modules ./node_modules

RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

EXPOSE 8080
CMD ["node","dist/main.js"]
