resource "google_compute_network" "vpc" {
  project                 = var.project_id
  name                    = "${var.name_prefix}-vpc"
  auto_create_subnetworks = false
  routing_mode            = "REGIONAL"
}

resource "google_compute_subnetwork" "subnet" {
  project                  = var.project_id
  name                     = "${var.name_prefix}-subnet"
  region                   = var.region
  network                  = google_compute_network.vpc.id
  ip_cidr_range            = var.subnet_cidr
  private_ip_google_access = true
}

resource "google_vpc_access_connector" "connector" {
  project       = var.project_id
  name          = "${var.name_prefix}-vpcconn"
  region        = var.region
  network       = google_compute_network.vpc.name
  ip_cidr_range = var.connector_cidr
  min_instances = 2
  max_instances = 3
  machine_type  = "e2-micro"
}

# Allow Cloud Run (via the Serverless VPC connector) to reach the data VM
# on MariaDB (3306) and Redis (6379). The connector creates instances with
# the network tag "vpc-connector" automatically; we match that here.
resource "google_compute_firewall" "allow_connector_to_data" {
  project = var.project_id
  name    = "${var.name_prefix}-allow-connector-to-data"
  network = google_compute_network.vpc.name

  direction     = "INGRESS"
  source_ranges = [var.connector_cidr]
  target_tags   = ["schemafy-data"]

  allow {
    protocol = "tcp"
    ports    = ["3306", "6379"]
  }
}

# Allow IAP SSH for operator access to the data VM.
resource "google_compute_firewall" "allow_iap_ssh" {
  project = var.project_id
  name    = "${var.name_prefix}-allow-iap-ssh"
  network = google_compute_network.vpc.name

  direction     = "INGRESS"
  source_ranges = ["35.235.240.0/20"] # Google IAP TCP forwarders
  target_tags   = ["schemafy-data"]

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}

# Permit egress to Google APIs / Container Registry for COS image pulls.
resource "google_compute_firewall" "allow_egress_all" {
  project = var.project_id
  name    = "${var.name_prefix}-allow-egress"
  network = google_compute_network.vpc.name

  direction          = "EGRESS"
  destination_ranges = ["0.0.0.0/0"]

  allow {
    protocol = "all"
  }
}

# Cloud NAT lets the data VM (no external IP) reach Docker Hub / package mirrors
# for first-boot startup script work.
resource "google_compute_router" "router" {
  project = var.project_id
  name    = "${var.name_prefix}-router"
  region  = var.region
  network = google_compute_network.vpc.id
}

resource "google_compute_router_nat" "nat" {
  project                            = var.project_id
  name                               = "${var.name_prefix}-nat"
  router                             = google_compute_router.router.name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"
}
