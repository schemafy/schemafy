resource "google_storage_bucket" "frontend" {
  project                     = var.project_id
  name                        = "${var.name_prefix}-frontend-${var.project_id}"
  location                    = var.region
  storage_class               = "STANDARD"
  uniform_bucket_level_access = true
  force_destroy               = false
  labels                      = var.labels

  website {
    main_page_suffix = "index.html"
    # SPA: serve index.html on 404 so client-side routing works.
    not_found_page = "index.html"
  }

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "OPTIONS"]
    response_header = ["*"]
    max_age_seconds = 3600
  }
}

# Public read for static assets (served via LB + Cloud CDN).
resource "google_storage_bucket_iam_member" "public_read" {
  bucket = google_storage_bucket.frontend.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}
