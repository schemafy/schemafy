resource "random_password" "db" {
  length  = 32
  special = false
}

resource "random_password" "db_root" {
  length  = 32
  special = false
}

resource "google_compute_address" "internal" {
  project      = var.project_id
  name         = "${var.name_prefix}-data-ip"
  region       = var.region
  subnetwork   = var.subnet_self_link
  address_type = "INTERNAL"
  purpose      = "GCE_ENDPOINT"
}

resource "google_compute_disk" "data" {
  project = var.project_id
  name    = "${var.name_prefix}-data-disk"
  type    = "pd-balanced"
  zone    = var.zone
  size    = var.data_disk_size_gb
  labels  = var.labels
}

resource "google_service_account" "vm" {
  project      = var.project_id
  account_id   = "${var.name_prefix}-data-vm"
  display_name = "Schemafy data VM"
}

resource "google_compute_instance" "data" {
  project      = var.project_id
  name         = "${var.name_prefix}-data"
  machine_type = var.machine_type
  zone         = var.zone
  tags         = ["schemafy-data"]
  labels       = var.labels

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 20
      type  = "pd-balanced"
    }
  }

  attached_disk {
    source      = google_compute_disk.data.self_link
    device_name = "schemafy-data"
    mode        = "READ_WRITE"
  }

  network_interface {
    subnetwork = var.subnet_self_link
    network_ip = google_compute_address.internal.address
    # Intentionally no access_config — VM has no external IP. NAT handles egress.
  }

  service_account {
    email  = google_service_account.vm.email
    scopes = ["cloud-platform"]
  }

  metadata = {
    enable-oslogin   = "TRUE"
    db-password      = random_password.db.result
    db-root-password = random_password.db_root.result
  }

  metadata_startup_script = templatefile("${path.module}/startup.sh.tftpl", {
    data_disk_name = "schemafy-data"
    db_name        = var.db_name
    db_user        = var.db_user
  })

  shielded_instance_config {
    enable_secure_boot          = true
    enable_vtpm                 = true
    enable_integrity_monitoring = true
  }

  lifecycle {
    ignore_changes = [
      # Boot image patches shouldn't recreate the VM.
      boot_disk[0].initialize_params[0].image,
    ]
  }
}
