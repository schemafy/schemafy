variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "name_prefix" {
  type = string
}

variable "subnet_cidr" {
  type    = string
  default = "10.10.0.0/24"
}

variable "connector_cidr" {
  type    = string
  default = "10.8.0.0/28"
}
