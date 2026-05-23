output "lb_ip" {
  description = "Public IP address of the global HTTPS load balancer."
  value       = module.load_balancer.ip_address
}

output "api_url" {
  description = "Cloud Run backend API service URL."
  value       = module.backend_api.uri
}

output "bff_url" {
  description = "Cloud Run BFF service URL."
  value       = module.bff.uri
}

output "frontend_bucket" {
  description = "GCS bucket name for the static frontend assets."
  value       = module.frontend_bucket.bucket_name
}

output "artifact_registry" {
  description = "Artifact Registry repository URL prefix (use as base for image tags)."
  value       = module.artifact_registry.repository_url
}

output "data_vm_internal_ip" {
  description = "Internal IP of the MariaDB/Redis VM."
  value       = module.data_vm.internal_ip
}

output "data_vm_name" {
  description = "Name of the data VM (for `gcloud compute ssh`)."
  value       = module.data_vm.instance_name
}

output "deployer_service_account" {
  description = "Service account email used by GitHub Actions WIF deploys."
  value       = module.ci_iam.deployer_sa_email
}

output "workload_identity_provider" {
  description = "Fully-qualified WIF provider name to use in `google-github-actions/auth`."
  value       = module.ci_iam.workload_identity_provider
}
