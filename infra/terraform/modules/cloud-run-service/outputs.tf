output "service_name" {
  value = google_cloud_run_v2_service.this.name
}

output "uri" {
  description = "Default Cloud Run URI (https://<name>-<hash>-<region>.run.app)."
  value       = google_cloud_run_v2_service.this.uri
}

output "location" {
  value = google_cloud_run_v2_service.this.location
}
