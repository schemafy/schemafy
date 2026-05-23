resource "google_secret_manager_secret" "secret" {
  for_each = toset(var.secret_names)

  project   = var.project_id
  secret_id = "${var.name_prefix}-${each.value}"
  labels    = var.labels

  replication {
    auto {}
  }
}
