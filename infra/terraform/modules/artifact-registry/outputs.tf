output "repository_id" {
  value = google_artifact_registry_repository.repo.repository_id
}

output "repository_url" {
  description = "Base URL for image tags (e.g. <region>-docker.pkg.dev/<project>/<repo>)."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.repo.repository_id}"
}
