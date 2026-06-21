output "app_image" {
  description = "Imagem Docker deployada"
  value       = "ghcr.io/${var.github_user}/siase-app:${var.image_tag}"
}

output "app_nodeport" {
  description = "Porta NodePort do app (mapeada para 8080 na VPS)"
  value       = 30080
}

output "namespace" {
  description = "Namespace Kubernetes"
  value       = "siase"
}
