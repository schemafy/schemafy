data "google_project" "this" {
  project_id = var.project_id
}

resource "google_iam_workload_identity_pool" "gh" {
  project                   = var.project_id
  workload_identity_pool_id = "${var.name_prefix}-gh-pool"
  display_name              = "GitHub Actions pool"
}

resource "google_iam_workload_identity_pool_provider" "gh" {
  project                            = var.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.gh.workload_identity_pool_id
  workload_identity_pool_provider_id = "${var.name_prefix}-gh-provider"
  display_name                       = "GitHub OIDC"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.repository" = "assertion.repository"
    "attribute.ref"        = "assertion.ref"
  }

  # Restrict tokens to the configured repo. Required since 2025: providers must
  # declare an attribute_condition or limit issuer audience.
  attribute_condition = "assertion.repository == \"${var.github_repo}\""

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account" "deployer" {
  project      = var.project_id
  account_id   = "${var.name_prefix}-deployer"
  display_name = "Schemafy GitHub Actions deployer"
}

# Allow the GitHub repo+ref to impersonate the deployer SA.
resource "google_service_account_iam_member" "wif_binding" {
  service_account_id = google_service_account.deployer.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.gh.name}/attribute.repository/${var.github_repo}"
}

# Deployer permissions: push images, deploy revisions, sync static assets,
# invalidate CDN, and impersonate runtime SA.
locals {
  deployer_roles = [
    "roles/artifactregistry.writer",
    "roles/run.admin",
    "roles/compute.loadBalancerAdmin", # url-maps invalidate-cdn-cache
    "roles/iam.serviceAccountUser",    # actAs runtime SA
  ]
}

resource "google_project_iam_member" "deployer" {
  for_each = toset(local.deployer_roles)
  project  = var.project_id
  role     = each.value
  member   = "serviceAccount:${google_service_account.deployer.email}"
}

# Frontend bucket write access scoped to that bucket.
resource "google_storage_bucket_iam_member" "deployer_frontend" {
  bucket = var.frontend_bucket_name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.deployer.email}"
}
