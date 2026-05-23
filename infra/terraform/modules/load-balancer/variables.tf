variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "name_prefix" {
  type = string
}

variable "domain" {
  description = "Custom domain for the managed certificate. Empty disables HTTPS (HTTP-only LB)."
  type        = string
  default     = ""
}

variable "frontend_bucket_name" {
  type = string
}

variable "api_service_name" {
  type = string
}

variable "bff_service_name" {
  type = string
}

variable "labels" {
  type    = map(string)
  default = {}
}
