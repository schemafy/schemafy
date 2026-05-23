variable "project_id" {
  description = "GCP project ID hosting all resources."
  type        = string
}

variable "region" {
  description = "Primary region for Cloud Run, Artifact Registry, VPC connector, GCE VM."
  type        = string
  default     = "asia-northeast3"
}

variable "zone" {
  description = "Compute zone for the data VM (must belong to var.region)."
  type        = string
  default     = "asia-northeast3-a"
}

variable "name_prefix" {
  description = "Prefix applied to all named resources (lowercase, alphanumeric+hyphen)."
  type        = string
  default     = "schemafy"
}

variable "domain" {
  description = "Custom domain served by the global HTTPS load balancer. Leave empty to skip managed cert + DNS managed zone (LB IP still provisioned)."
  type        = string
  default     = ""
}

variable "manage_dns_zone" {
  description = "If true and var.domain is set, create a Cloud DNS managed zone for the domain. Set false if DNS lives elsewhere."
  type        = bool
  default     = false
}

variable "github_repo" {
  description = "GitHub repo allowed to assume the deployer SA via Workload Identity Federation (owner/repo)."
  type        = string
  default     = "yan-su/schemafy"
}

variable "github_ref" {
  description = "Git ref (branch) allowed to deploy. Default restricts deploys to main."
  type        = string
  default     = "refs/heads/main"
}

variable "data_vm_machine_type" {
  description = "Machine type for the MariaDB/Redis VM."
  type        = string
  default     = "e2-small"
}

variable "data_vm_disk_size_gb" {
  description = "Persistent data disk size in GB (mounted at /var/lib/schemafy)."
  type        = number
  default     = 50
}

variable "bff_image" {
  description = "Container image for the BFF Cloud Run service. Use a placeholder for the first apply."
  type        = string
  default     = "gcr.io/cloudrun/hello"
}

variable "api_image" {
  description = "Container image for the backend API Cloud Run service. Use a placeholder for the first apply."
  type        = string
  default     = "gcr.io/cloudrun/hello"
}

variable "bff_min_instances" {
  type    = number
  default = 1
}

variable "bff_max_instances" {
  type    = number
  default = 3
}

variable "api_min_instances" {
  type    = number
  default = 1
}

variable "api_max_instances" {
  type    = number
  default = 5
}

variable "bff_cpu" {
  type    = string
  default = "1"
}

variable "bff_memory" {
  type    = string
  default = "512Mi"
}

variable "api_cpu" {
  type    = string
  default = "1"
}

variable "api_memory" {
  type    = string
  default = "1Gi"
}
