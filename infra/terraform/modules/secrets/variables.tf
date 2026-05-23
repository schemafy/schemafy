variable "project_id" {
  type = string
}

variable "name_prefix" {
  type = string
}

variable "secret_names" {
  description = "Logical secret names (will be prefixed). Values are populated manually post-apply."
  type        = list(string)
}

variable "labels" {
  type    = map(string)
  default = {}
}
