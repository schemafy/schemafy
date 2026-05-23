# Schemafy GCP 배포 가이드

이 문서는 `infra/terraform`을 사용해 Schemafy를 GCP에 배포하는 절차를 설명합니다.

## 아키텍처 요약

```
   사용자 → Global HTTPS LB ┬─ /         → GCS bucket (frontend SPA, Cloud CDN)
                           ├─ /api/*    → Cloud Run: schemafy-api (Spring Boot)
                           └─ /bff/*    → Cloud Run: schemafy-bff (NestJS, WebSocket)
                                          │
                                          └─ Serverless VPC connector
                                              │
                                              └─ GCE VM (e2-small)
                                                  ├─ MariaDB 11.4 (3306)
                                                  └─ Redis 8.4 (6379)
```

- 리전: `asia-northeast3` (서울)
- 환경: 단일 prod
- 시크릿: Secret Manager
- 컨테이너 이미지: Artifact Registry (`schemafy` 리포)
- CI/CD: GitHub Actions + Workload Identity Federation

## 사전 준비

1. GCP project 생성 및 billing 연결
2. 로컬에 `gcloud` CLI 설치, `gcloud auth login` + `gcloud auth application-default login`
3. Terraform >= 1.6 설치
4. (선택) 도메인 보유

## 1. Bootstrap (1회만)

```bash
export PROJECT_ID=my-gcp-project
export REGION=asia-northeast3
./infra/bootstrap.sh
```

이 스크립트가 하는 일:
- 필요한 GCP API 활성화
- Terraform state 버킷 `gs://<project>-tf-state` 생성 (버저닝 on)

## 2. Terraform 첫 apply

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # project_id 등 편집
terraform init -backend-config="bucket=${PROJECT_ID}-tf-state"
terraform plan
terraform apply
```

이 단계에서 만들어지는 것:
- VPC + Serverless VPC connector + NAT
- 데이터 VM (MariaDB + Redis, docker-compose) — 시작 스크립트 실행에 ~3분
- Artifact Registry 리포
- Cloud Run 서비스 2개 (BFF, API) — **placeholder 이미지(`gcr.io/cloudrun/hello`)로 기동**
- Secret Manager 시크릿 7개 (`db-password`는 자동 주입, 나머지는 빈 상태)
- GCS 프론트 버킷 + Cloud CDN
- Global HTTPS LB (도메인 미설정 시 HTTP-only)
- GitHub Actions용 WIF pool/provider + deployer SA

출력값 확인:

```bash
terraform output
```

`lb_ip`, `artifact_registry`, `workload_identity_provider`, `deployer_service_account`를 기록해두세요.

## 3. 시크릿 값 주입 (1회)

`db-password`는 Terraform이 랜덤 생성해 자동으로 주입합니다. 나머지 시크릿은 수동 주입이 필요합니다:

```bash
SECRETS=(
  schemafy-hmac-secret
  schemafy-hmac-previous-secret
  schemafy-github-client-id
  schemafy-github-client-secret
  schemafy-jwt-secret
  schemafy-sharelink-pepper
)

# 예시: HMAC 시크릿 생성
openssl rand -base64 48 | gcloud secrets versions add schemafy-hmac-secret --data-file=-

# 예시: GitHub OAuth 앱에서 발급받은 값 주입
echo -n "Iv1.abc..." | gcloud secrets versions add schemafy-github-client-id --data-file=-
echo -n "<secret>" | gcloud secrets versions add schemafy-github-client-secret --data-file=-
```

값 주입 후 Cloud Run 서비스를 한 번 재배포하면 새 시크릿 버전이 마운트됩니다 (다음 단계의 첫 GitHub Actions deploy에서 자동으로 일어남).

## 4. GitHub Actions 시크릿 설정

GitHub repo > Settings > Secrets and variables > Actions에서:

| 이름 | 값 |
|---|---|
| `GCP_PROJECT_ID` | `terraform output -raw project_id` (또는 직접 값) |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `terraform output -raw workload_identity_provider` |
| `GCP_DEPLOYER_SA` | `terraform output -raw deployer_service_account` |
| `GCP_REGION` | `asia-northeast3` |
| `GCP_AR_REPO` | `schemafy` |
| `FRONTEND_BUCKET` | `terraform output -raw frontend_bucket` |
| `LB_URL_MAP` | `schemafy-url-map` (CDN invalidate용) |

## 5. 첫 실배포

`main`에 푸시하거나 `.github/workflows/deploy.yml`을 `workflow_dispatch`로 수동 실행하면:
- 변경 감지에 따라 frontend/bff/api 잡이 병렬 실행
- BFF/API 이미지가 Artifact Registry에 푸시되고 Cloud Run revision으로 배포됨
- frontend 정적 자산이 GCS에 sync되고 CDN cache 무효화

## 6. 도메인 연결 (선택)

도메인이 있다면:

1. `terraform.tfvars`에서 `domain = "schemafy.example.com"` 설정
2. (DNS도 GCP에서 관리할 경우) `manage_dns_zone = true`
3. `terraform apply`
4. 외부 DNS에서 A 레코드를 `lb_ip` 출력값으로 연결 (또는 GCP DNS가 관리)
5. 관리형 인증서 발급에 10~30분 소요. 진행상황:
   ```bash
   gcloud compute ssl-certificates describe schemafy-cert
   ```

## 운영 작업

### 데이터 VM 접속 (디버깅)

```bash
gcloud compute ssh schemafy-data --tunnel-through-iap --zone=asia-northeast3-a
# VM 내부에서:
sudo docker ps
sudo docker logs schemafy-mariadb
```

### DB 백업

`/var/lib/schemafy/mariadb`가 persistent disk에 있으므로 디스크 스냅샷으로 백업:

```bash
gcloud compute disks snapshot schemafy-data-disk \
  --zone=asia-northeast3-a \
  --snapshot-names=schemafy-data-$(date +%Y%m%d)
```

자동화는 Cloud Scheduler + snapshot policy 추가로 가능 (현재는 수동).

### 롤백

Cloud Run revision은 자동 유지되므로 즉시 롤백 가능:

```bash
gcloud run services update-traffic schemafy-api \
  --to-revisions=<previous-revision>=100 \
  --region=asia-northeast3
```

### 인프라 변경

`infra/terraform/*.tf` 수정 → PR → `.github/workflows/terraform.yml`이 PR에 plan 댓글 → main merge 시 apply.

## 비용 (추정)

| 리소스 | 월 비용 (대략) |
|---|---|
| e2-small VM + 50GB pd-balanced | ~$15 |
| Global HTTPS LB + 1 forwarding rule | ~$18 |
| Cloud Run (1 min-instance × 2 서비스, idle) | ~$15 |
| Cloud CDN, Artifact Registry, NAT, etc. | ~$5–15 |
| **합계** | **~$50–65/월** |

스케일 시 Cloud Run 인스턴스 시간이 주요 변동 비용.
