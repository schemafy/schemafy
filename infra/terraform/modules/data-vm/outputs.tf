output "internal_ip" {
  value = google_compute_address.internal.address
}

output "instance_name" {
  value = google_compute_instance.data.name
}

output "db_password" {
  description = "Randomly generated DB password. Use this to populate the Secret Manager secret consumed by Cloud Run."
  value       = random_password.db.result
  sensitive   = true
}

output "db_root_password" {
  value     = random_password.db_root.result
  sensitive = true
}
