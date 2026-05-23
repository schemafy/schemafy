variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "name" {
  type = string
}

variable "image" {
  type = string
}

variable "port" {
  type    = number
  default = 8080
}

variable "service_account_email" {
  type = string
}

variable "vpc_connector" {
  type = string
}

variable "min_instances" {
  type    = number
  default = 0
}

variable "max_instances" {
  type    = number
  default = 5
}

variable "cpu" {
  type    = string
  default = "1"
}

variable "memory" {
  type    = string
  default = "512Mi"
}

variable "env" {
  description = "Plain (non-secret) environment variables."
  type        = map(string)
  default     = {}
}

variable "secret_env" {
  description = "Map of env var name -> Secret Manager secret ID (projects/<num>/secrets/<id>). Uses latest version."
  type        = map(string)
  default     = {}
}

variable "health_check_path" {
  type    = string
  default = "/"
}

variable "request_timeout" {
  type    = string
  default = "60s"
}

variable "labels" {
  type    = map(string)
  default = {}
}
