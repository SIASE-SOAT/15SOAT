variable "image_tag" {
  description = "Tag da imagem Docker do app (git SHA)"
  type        = string
}

variable "github_user" {
  description = "Username ou org do GitHub (para ghcr.io)"
  type        = string
  default     = "ricardo-okuyama"
}

variable "db_password" {
  description = "Senha do banco de dados PostgreSQL"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "Segredo JWT em base64"
  type        = string
  sensitive   = true
}

variable "webhook_token" {
  description = "Token para autenticação de webhooks"
  type        = string
  sensitive   = true
}

variable "mecanico_password" {
  description = "Senha seed do mecanico"
  type        = string
  sensitive   = true
}

variable "app_replicas" {
  description = "Número inicial de réplicas do app"
  type        = number
  default     = 2
}

variable "kubeconfig_path" {
  description = "Caminho do kubeconfig"
  type        = string
  default     = "~/.kube/config"
}
