output "network_self_link" {
  value = google_compute_network.vpc.self_link
}

output "subnet_self_link" {
  value = google_compute_subnetwork.subnet.self_link
}

output "vpc_connector_id" {
  value = google_vpc_access_connector.connector.id
}

output "data_tag" {
  description = "Network tag attached to the data VM and matched by firewall rules."
  value       = "schemafy-data"
}
