output "secret_ids" {
  description = "Map of logical secret name -> Secret Manager resource ID (projects/<num>/secrets/<id>)."
  value       = { for k, s in google_secret_manager_secret.secret : k => s.id }
}

output "secret_names" {
  description = "Map of logical secret name -> Secret Manager short name (for use with `gcloud secrets`)."
  value       = { for k, s in google_secret_manager_secret.secret : k => s.secret_id }
}
