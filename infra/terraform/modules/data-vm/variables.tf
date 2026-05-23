variable "project_id" {
  type = string
}

variable "region" {
  type = string
}

variable "zone" {
  type = string
}

variable "name_prefix" {
  type = string
}

variable "network_self_link" {
  type = string
}

variable "subnet_self_link" {
  type = string
}

variable "machine_type" {
  type    = string
  default = "e2-small"
}

variable "data_disk_size_gb" {
  type    = number
  default = 50
}

variable "labels" {
  type    = map(string)
  default = {}
}

variable "db_name" {
  type    = string
  default = "schemafy"
}

variable "db_user" {
  type    = string
  default = "schemafy"
}
