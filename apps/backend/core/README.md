# Spring Boot 애플리케이션 실행 가이드

## 사전 요구사항

- Java 21
- Apple Silicon Mac의 경우 Rosetta 2 (필요한 경우에만)
- node version 18 이상
- podman, podman compose

## 초기 설정 (최초 1회만)

### 1. Rosetta 2 설치

spring 실행 시 `Execution failed for task ':generateProto'` 발생할 경우 아래 명령어로 설치

```
softwareupdate --install-rosetta --agree-to-license
```

### 2. 환경 변수 파일 생성

프로젝트 루트에 `.env` 파일을 생성합니다:

`.env` 예시 파일 내용:
```
# Database
DB_NAME=schemafy
DB_USER=schemafy
DB_PASSWORD=schemafy
DB_ROOT_PASSWORD={your_password}
DB_PORT=3306

# Redis
REDIS_PORT=6379

# Timezone
TZ=Asia/Seoul
```

## 애플리케이션 실행

### 1. podman, podman-infra 설치

- Podman 설치
```bash
brew install podman
```

- Podman Compose 설치
```bash 
brew install podman-compose
```

- Podman 머신 초기화 및 시작
```bash
podman machine init
podman machine start
```

### 2. infra 실행

```bash
npm run infra
```

### 3. spring boot 실행
