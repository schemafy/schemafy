output "ip_address" {
  value = google_compute_global_address.lb.address
}

output "url_map_name" {
  value = google_compute_url_map.https.name
}
