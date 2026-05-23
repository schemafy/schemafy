#!/usr/bin/env bash
# One-time GCP bootstrap for Schemafy Terraform deploy.
# Idempotent: safe to re-run. Requires gcloud auth as a Project Owner.
#
# Usage:
#   PROJECT_ID=my-gcp-project REGION=asia-northeast3 ./infra/bootstrap.sh
set -euo pipefail

PROJECT_ID=${PROJECT_ID:?PROJECT_ID env var is required}
REGION=${REGION:-asia-northeast3}
STATE_BUCKET=${STATE_BUCKET:-${PROJECT_ID}-tf-state}

echo ">> Setting active project to $PROJECT_ID"
gcloud config set project "$PROJECT_ID"

echo ">> Enabling required APIs"
gcloud services enable \
  compute.googleapis.com \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  iam.googleapis.com \
  cloudresourcemanager.googleapis.com \
  vpcaccess.googleapis.com \
  dns.googleapis.com \
  certificatemanager.googleapis.com \
  iamcredentials.googleapis.com \
  sts.googleapis.com \
  storage.googleapis.com \
  --project="$PROJECT_ID"

echo ">> Creating Terraform state bucket gs://$STATE_BUCKET (if absent)"
if ! gcloud storage buckets describe "gs://$STATE_BUCKET" --project="$PROJECT_ID" >/dev/null 2>&1; then
  gcloud storage buckets create "gs://$STATE_BUCKET" \
    --project="$PROJECT_ID" \
    --location="$REGION" \
    --uniform-bucket-level-access \
    --public-access-prevention
  gcloud storage buckets update "gs://$STATE_BUCKET" --versioning
fi

cat <<EOF

Bootstrap complete.

Next steps:
  cd infra/terraform
  cp terraform.tfvars.example terraform.tfvars   # edit project_id, domain, etc.
  terraform init -backend-config="bucket=$STATE_BUCKET"
  terraform apply

After the first apply, see docs/DEPLOYMENT.md for secret injection and
GitHub Actions secret wiring.
EOF
