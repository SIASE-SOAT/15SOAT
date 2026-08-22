variable "image_tag" {
  type = string
}

variable "ecr_repository" {
  type = string
}

variable "ecr_dockerconfigjson" {
  type      = string
  sensitive = true
}

variable "db_host" {
  type = string
}

variable "db_port" {
  type    = string
  default = "5432"
}

variable "db_name" {
  type    = string
  default = "siase_db"
}

variable "db_user" {
  type    = string
  default = "siase_user"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "jwt_issuer" {
  type    = string
  default = "siase-auth"
}

variable "webhook_token" {
  type      = string
  sensitive = true
}

variable "mecanico_password" {
  type      = string
  sensitive = true
}

variable "app_replicas" {
  type    = number
  default = 2
}

variable "kubeconfig_path" {
  type    = string
  default = "~/.kube/config"
}

variable "cors_allowed_origins" {
  type    = string
  default = "https://siase-frontend.vercel.app"
}

variable "cliente_portal_url" {
  type    = string
  default = "https://siase-frontend.vercel.app"
}
