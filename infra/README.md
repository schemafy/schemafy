# Infra

GCP Cloud Run 배포를 위한 Docker 빌드 및 Terraform 인프라 구성.

## Docker 빌드

모든 빌드 명령은 **프로젝트 루트**에서 실행해야 합니다.

### Backend (Spring Boot)

```bash
docker build -f infra/docker/backend/Dockerfile -t schemafy-backend .
```

### BFF (NestJS)

```bash
docker build -f infra/docker/bff/Dockerfile -t schemafy-bff .
```

### Frontend (React + nginx)

```bash
docker build -f infra/docker/frontend/Dockerfile -t schemafy-frontend .
```

## 로컬 실행

빌드된 이미지를 로컬에서 실행하려면 먼저 MariaDB와 Redis가 필요합니다.

```bash
# 인프라 (MariaDB + Redis) 실행
docker compose up -d

# Backend 실행 (포트 8080)
docker run --rm --network host \
  -e DB_HOST=localhost -e DB_PORT=3306 \
  -e DB_NAME=schemafy -e DB_USER=schemafy -e DB_PASSWORD=schemafy \
  -e REDIS_HOST=localhost -e REDIS_PORT=6379 \
  schemafy-backend

# BFF 실행 (포트 4000)
docker run --rm --network host \
  -e BACKEND_URL=http://localhost:8080 \
  -e FRONTEND_URL=http://localhost:3001 \
  schemafy-bff

# Frontend 실행 (포트 8080 → 충돌 방지를 위해 3001로 매핑)
docker run --rm -p 3001:8080 schemafy-frontend
```

## 참고

- Frontend Dockerfile은 `package-lock.json`을 의도적으로 제외합니다. macOS에서 생성된 lockfile에 Linux용 rollup 바이너리가 포함되지 않는 npm 알려진 이슈 때문입니다.
- Frontend 빌드 시 `zod` 의존성이 `apps/frontend/package.json`에 추가되어 있어야 합니다.
