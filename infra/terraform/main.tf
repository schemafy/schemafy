locals {
  common_labels = {
    app        = "schemafy"
    managed_by = "terraform"
    env        = "prod"
  }

  # Application env (non-secret). DB_HOST/REDIS_HOST are wired from the data VM module.
  common_env = {
    TZ                           = "Asia/Seoul"
    DB_PORT                      = "3306"
    DB_NAME                      = "schemafy"
    DB_USER                      = "schemafy"
    REDIS_PORT                   = "6379"
    REDIS_ENABLED                = "true"
    GITHUB_REDIRECT_URI          = var.domain != "" ? "https://${var.domain}/public/api/v1.0/oauth/github/callback" : ""
    GITHUB_FRONTEND_CALLBACK_URL = var.domain != "" ? "https://${var.domain}/oauth/callback" : ""
    FRONTEND_URL                 = var.domain != "" ? "https://${var.domain}" : ""
    JWT_ALLOWED_ORIGINS          = var.domain != "" ? "https://${var.domain}" : ""
  }

  # Secret names created in Secret Manager; values are populated manually post-apply.
  secret_names = [
    "db-password",
    "hmac-secret",
    "hmac-previous-secret",
    "github-client-id",
    "github-client-secret",
    "jwt-secret",
    "sharelink-pepper",
  ]
}

module "network" {
  source      = "./modules/network"
  project_id  = var.project_id
  region      = var.region
  name_prefix = var.name_prefix
}

module "artifact_registry" {
  source      = "./modules/artifact-registry"
  project_id  = var.project_id
  region      = var.region
  name_prefix = var.name_prefix
}

module "data_vm" {
  source            = "./modules/data-vm"
  project_id        = var.project_id
  region            = var.region
  zone              = var.zone
  name_prefix       = var.name_prefix
  network_self_link = module.network.network_self_link
  subnet_self_link  = module.network.subnet_self_link
  machine_type      = var.data_vm_machine_type
  data_disk_size_gb = var.data_vm_disk_size_gb
  labels            = local.common_labels
}

module "secrets" {
  source       = "./modules/secrets"
  project_id   = var.project_id
  name_prefix  = var.name_prefix
  secret_names = local.secret_names
  labels       = local.common_labels
}

module "frontend_bucket" {
  source      = "./modules/frontend-bucket"
  project_id  = var.project_id
  region      = var.region
  name_prefix = var.name_prefix
  labels      = local.common_labels
}

# Runtime service account shared by both Cloud Run services.
resource "google_service_account" "runtime" {
  project      = var.project_id
  account_id   = "${var.name_prefix}-run"
  display_name = "Schemafy Cloud Run runtime"
}

# Populate db-password with the random value generated for the data VM so
# Cloud Run and the VM share the same MariaDB credentials without manual sync.
resource "google_secret_manager_secret_version" "db_password" {
  secret      = module.secrets.secret_ids["db-password"]
  secret_data = module.data_vm.db_password
}

# Grant runtime SA access to every secret created above.
resource "google_secret_manager_secret_iam_member" "runtime_access" {
  for_each  = toset(local.secret_names)
  project   = var.project_id
  secret_id = module.secrets.secret_ids[each.value]
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.runtime.email}"
}

module "backend_api" {
  source = "./modules/cloud-run-service"

  project_id            = var.project_id
  region                = var.region
  name                  = "${var.name_prefix}-api"
  image                 = var.api_image
  port                  = 8080
  service_account_email = google_service_account.runtime.email
  vpc_connector         = module.network.vpc_connector_id
  min_instances         = var.api_min_instances
  max_instances         = var.api_max_instances
  cpu                   = var.api_cpu
  memory                = var.api_memory
  health_check_path     = "/actuator/health"
  labels                = local.common_labels

  env = merge(local.common_env, {
    DB_HOST                = module.data_vm.internal_ip
    REDIS_HOST             = module.data_vm.internal_ip
    SPRING_PROFILES_ACTIVE = "server"
  })

  secret_env = {
    DB_PASSWORD          = module.secrets.secret_ids["db-password"]
    HMAC_SECRET          = module.secrets.secret_ids["hmac-secret"]
    HMAC_PREVIOUS_SECRET = module.secrets.secret_ids["hmac-previous-secret"]
    GITHUB_CLIENT_ID     = module.secrets.secret_ids["github-client-id"]
    GITHUB_CLIENT_SECRET = module.secrets.secret_ids["github-client-secret"]
    JWT_SECRET           = module.secrets.secret_ids["jwt-secret"]
    SHARELINK_PEPPER     = module.secrets.secret_ids["sharelink-pepper"]
  }

  depends_on = [google_secret_manager_secret_iam_member.runtime_access]
}

module "bff" {
  source = "./modules/cloud-run-service"

  project_id            = var.project_id
  region                = var.region
  name                  = "${var.name_prefix}-bff"
  image                 = var.bff_image
  port                  = 8080
  service_account_email = google_service_account.runtime.email
  vpc_connector         = module.network.vpc_connector_id
  min_instances         = var.bff_min_instances
  max_instances         = var.bff_max_instances
  cpu                   = var.bff_cpu
  memory                = var.bff_memory
  health_check_path     = "/health"
  request_timeout       = "3600s" # WebSocket support
  labels                = local.common_labels

  env = merge(local.common_env, {
    BACKEND_URL = module.backend_api.uri
    NODE_ENV    = "production"
  })

  secret_env = {
    HMAC_SECRET = module.secrets.secret_ids["hmac-secret"]
  }

  depends_on = [google_secret_manager_secret_iam_member.runtime_access]
}

module "load_balancer" {
  source = "./modules/load-balancer"

  project_id           = var.project_id
  region               = var.region
  name_prefix          = var.name_prefix
  domain               = var.domain
  frontend_bucket_name = module.frontend_bucket.bucket_name
  api_service_name     = module.backend_api.service_name
  bff_service_name     = module.bff.service_name
  labels               = local.common_labels
}

module "ci_iam" {
  source = "./modules/ci-iam"

  project_id           = var.project_id
  name_prefix          = var.name_prefix
  github_repo          = var.github_repo
  github_ref           = var.github_ref
  runtime_sa_email     = google_service_account.runtime.email
  frontend_bucket_name = module.frontend_bucket.bucket_name
  artifact_repo_id     = module.artifact_registry.repository_id
}

# Optional DNS zone (only if domain set + manage_dns_zone true).
resource "google_dns_managed_zone" "primary" {
  count    = var.domain != "" && var.manage_dns_zone ? 1 : 0
  project  = var.project_id
  name     = "${var.name_prefix}-zone"
  dns_name = "${var.domain}."
  labels   = local.common_labels
}

resource "google_dns_record_set" "apex" {
  count        = var.domain != "" && var.manage_dns_zone ? 1 : 0
  project      = var.project_id
  managed_zone = google_dns_managed_zone.primary[0].name
  name         = "${var.domain}."
  type         = "A"
  ttl          = 300
  rrdatas      = [module.load_balancer.ip_address]
}
