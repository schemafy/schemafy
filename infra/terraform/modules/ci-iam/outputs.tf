output "deployer_sa_email" {
  value = google_service_account.deployer.email
}

output "workload_identity_provider" {
  description = "Pass this to google-github-actions/auth as `workload_identity_provider`."
  value       = "projects/${data.google_project.this.number}/locations/global/workloadIdentityPools/${google_iam_workload_identity_pool.gh.workload_identity_pool_id}/providers/${google_iam_workload_identity_pool_provider.gh.workload_identity_pool_provider_id}"
}
