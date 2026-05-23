locals {
  use_https = var.domain != ""
}

resource "google_compute_global_address" "lb" {
  project = var.project_id
  name    = "${var.name_prefix}-lb-ip"
}

# Backend bucket (frontend static assets) with Cloud CDN.
resource "google_compute_backend_bucket" "frontend" {
  project     = var.project_id
  name        = "${var.name_prefix}-frontend-backend"
  bucket_name = var.frontend_bucket_name
  enable_cdn  = true

  cdn_policy {
    cache_mode        = "CACHE_ALL_STATIC"
    client_ttl        = 3600
    default_ttl       = 3600
    max_ttl           = 86400
    negative_caching  = true
    serve_while_stale = 86400
  }
}

# Serverless NEGs pointing at each Cloud Run service.
resource "google_compute_region_network_endpoint_group" "api" {
  project               = var.project_id
  name                  = "${var.name_prefix}-api-neg"
  region                = var.region
  network_endpoint_type = "SERVERLESS"

  cloud_run {
    service = var.api_service_name
  }
}

resource "google_compute_region_network_endpoint_group" "bff" {
  project               = var.project_id
  name                  = "${var.name_prefix}-bff-neg"
  region                = var.region
  network_endpoint_type = "SERVERLESS"

  cloud_run {
    service = var.bff_service_name
  }
}

resource "google_compute_backend_service" "api" {
  project               = var.project_id
  name                  = "${var.name_prefix}-api-backend"
  protocol              = "HTTPS"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  # timeout_sec is not supported for serverless NEGs; Cloud Run controls
  # request timeout at the service level.

  backend {
    group = google_compute_region_network_endpoint_group.api.id
  }

  log_config {
    enable      = true
    sample_rate = 1.0
  }
}

resource "google_compute_backend_service" "bff" {
  project               = var.project_id
  name                  = "${var.name_prefix}-bff-backend"
  protocol              = "HTTPS"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  # WebSocket sessions ride on Cloud Run's request_timeout (3600s).

  backend {
    group = google_compute_region_network_endpoint_group.bff.id
  }

  log_config {
    enable      = true
    sample_rate = 1.0
  }
}

resource "google_compute_url_map" "https" {
  project         = var.project_id
  name            = "${var.name_prefix}-url-map"
  default_service = google_compute_backend_bucket.frontend.id

  host_rule {
    hosts        = ["*"]
    path_matcher = "main"
  }

  path_matcher {
    name            = "main"
    default_service = google_compute_backend_bucket.frontend.id

    path_rule {
      paths   = ["/api", "/api/*"]
      service = google_compute_backend_service.api.id
    }

    # Backend HMAC filter only skips signatures for paths under /public/api/.
    # Route the public auth/OAuth surface directly so login/signup/refresh and
    # the GitHub OAuth callback can reach the backend without HMAC headers.
    path_rule {
      paths   = ["/public", "/public/*"]
      service = google_compute_backend_service.api.id
    }

    path_rule {
      paths   = ["/bff", "/bff/*"]
      service = google_compute_backend_service.bff.id
    }

    path_rule {
      paths   = ["/ws", "/ws/*"]
      service = google_compute_backend_service.bff.id
    }
  }
}

# HTTP listener — either serves directly (no domain) or redirects to HTTPS.
resource "google_compute_url_map" "http_redirect" {
  count   = local.use_https ? 1 : 0
  project = var.project_id
  name    = "${var.name_prefix}-http-redirect"

  default_url_redirect {
    https_redirect         = true
    redirect_response_code = "MOVED_PERMANENTLY_DEFAULT"
    strip_query            = false
  }
}

resource "google_compute_target_http_proxy" "http" {
  project = var.project_id
  name    = "${var.name_prefix}-http-proxy"
  url_map = local.use_https ? google_compute_url_map.http_redirect[0].id : google_compute_url_map.https.id
}

resource "google_compute_global_forwarding_rule" "http" {
  project               = var.project_id
  name                  = "${var.name_prefix}-fr-http"
  ip_address            = google_compute_global_address.lb.id
  ip_protocol           = "TCP"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  port_range            = "80"
  target                = google_compute_target_http_proxy.http.id
}

# Managed certificate + HTTPS listener (only when domain is configured).
resource "google_compute_managed_ssl_certificate" "cert" {
  count   = local.use_https ? 1 : 0
  project = var.project_id
  name    = "${var.name_prefix}-cert"

  managed {
    domains = [var.domain]
  }
}

resource "google_compute_target_https_proxy" "https" {
  count            = local.use_https ? 1 : 0
  project          = var.project_id
  name             = "${var.name_prefix}-https-proxy"
  url_map          = google_compute_url_map.https.id
  ssl_certificates = [google_compute_managed_ssl_certificate.cert[0].id]
}

resource "google_compute_global_forwarding_rule" "https" {
  count                 = local.use_https ? 1 : 0
  project               = var.project_id
  name                  = "${var.name_prefix}-fr-https"
  ip_address            = google_compute_global_address.lb.id
  ip_protocol           = "TCP"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  port_range            = "443"
  target                = google_compute_target_https_proxy.https[0].id
}
