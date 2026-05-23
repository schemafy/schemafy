output "bucket_name" {
  value = google_storage_bucket.frontend.name
}

output "bucket_self_link" {
  value = google_storage_bucket.frontend.self_link
}
