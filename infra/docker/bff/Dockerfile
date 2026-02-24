# Stage 1: Build
FROM node:22-alpine AS builder

WORKDIR /app

# Copy root workspace files (exclude lockfile to get correct platform-specific deps)
COPY package.json ./
COPY apps/bff/package.json ./apps/bff/package.json

RUN npm install --workspace=apps/bff

COPY apps/bff/ ./apps/bff/

RUN npm run build --workspace=apps/bff

# Stage 2: Run
FROM node:22-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/apps/bff/package.json ./
COPY --from=builder /app/apps/bff/dist/ ./dist/
COPY --from=builder /app/node_modules/ ./node_modules/

RUN chown -R appuser:appgroup /app
USER appuser

ENV NODE_ENV=production

EXPOSE 4000

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=10s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4000/ || exit 1

CMD ["node", "dist/main.js"]
