# Remote state stored in a GCS bucket created by infra/bootstrap.sh.
# Run `terraform init -backend-config="bucket=<your-state-bucket>"` for first init.
terraform {
  backend "gcs" {
    prefix = "schemafy/prod"
  }
}
