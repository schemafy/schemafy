variable "project_id" {
  type = string
}

variable "name_prefix" {
  type = string
}

variable "github_repo" {
  description = "GitHub repository in owner/name form."
  type        = string
}

variable "github_ref" {
  description = "Refs allowed to deploy. e.g. refs/heads/main"
  type        = string
  default     = "refs/heads/main"
}

variable "runtime_sa_email" {
  description = "Cloud Run runtime SA that the deployer impersonates via iam.serviceAccountUser."
  type        = string
}

variable "frontend_bucket_name" {
  type = string
}

variable "artifact_repo_id" {
  type = string
}
